import csv
import pickle
import shapefile
import xml.etree.cElementTree as eTree
from datetime import datetime as time
from math import sin, asin, cos, radians, fabs, sqrt
import matplotlib.pyplot as plt
# population data process
# try:

# except ImportError:
#     import xml.etree.ElementTree as ET
import null as null


class Coordinate:
    def __init__(self, x, y):
        self.x = x
        self.y = y


class Position:
    def __init__(self, x, y, zipcode):
        self.x = x
        self.y = y
        self.zipcode = zipcode


EARTH_RADIUS = 6371000


def hav(theta):
    s = sin(theta / 2)
    return s * s


def get_distance_hav(lat0, lng0, lat1, lng1):
    lat0 = radians(lat0)
    lat1 = radians(lat1)
    lng0 = radians(lng0)
    lng1 = radians(lng1)
    dlng = fabs(lng0 - lng1)
    dlat = fabs(lat0 - lat1)
    h = hav(dlat) + cos(lat0) * cos(lat1) * hav(dlng)
    distance = 2 * EARTH_RADIUS * asin(sqrt(h))
    return distance

with open('xmlFiles/positions.pickle', 'rb') as file:
    positions = pickle.load(file)

def get_depart_time(activity):
    depart_hour = int(activity[40]) if int(activity[40]) < 24 else 0
    if depart_hour > 3:
        depart_time = begin_date + " " + str(depart_hour) + ":" + activity[41]
    else:
        depart_time = next_date + " " + str(depart_hour) + ":" + activity[41]
    depart_time = time.strptime(depart_time, '%Y-%m-%d %H:%M')
    depart_time = time.strftime(depart_time, '%Y-%m-%d %H:%M')
    return depart_time


def get_arrive_time(activity):
    arrive_hour = int(activity[38])
    arrive_hour = arrive_hour if arrive_hour < 24 else 0
    if arrive_hour > 3:
        arrive_time = begin_date + " " + str(arrive_hour) + ":" + activity[39]
    else:
        arrive_time = next_date + " " + str(arrive_hour) + ":" + activity[39]
    arrive_time = time.strptime(arrive_time, '%Y-%m-%d %H:%M')
    arrive_time = time.strftime(arrive_time, '%Y-%m-%d %H:%M')
    return arrive_time


def entry_convert(acticvity_list, person_ID, entry, zipcode, tract):
    # activity_mask: household id + person no + day no
    eTree.SubElement(entry, "ID").text = person_ID
    person = eTree.SubElement(entry, "person",
                              {"houseHoldID": acticvity_list[0][2], "zipcode": zipcode, "tract": tract})
    if len(acticvity_list) == 1:
        return False
    if tract not in tract_Names:
        return False
    distance = 0
    for count in range(len(acticvity_list) - 1):
        activity = acticvity_list[count]
        next_activity = acticvity_list[count + 1]
        taskID = person_ID + activity[5] + next_activity[5]
        depart_ID = activity[6]
        arrive_ID = next_activity[6]

        purposeID = next_activity[7]
        depart_time = get_depart_time(activity)
        arrive_time = get_arrive_time(next_activity)
        if depart_time >= arrive_time or (depart_ID not in positions.keys()) or (arrive_ID not in positions.keys()):
            return False
        if positions[depart_ID].x == positions[arrive_ID].x and positions[depart_ID].y == positions[arrive_ID].y:
            continue
        task = eTree.SubElement(person, "task",
                                {"taskID": taskID, "departTime": depart_time, "arriveTime": arrive_time,
                                 "purposeId": purposeID})
        eTree.SubElement(task, "departCoord",
                         {"longitude": positions[depart_ID].x, "latitude": positions[depart_ID].y})
        eTree.SubElement(task, "arriveCoord",
                         {"longitude": positions[arrive_ID].x, "latitude": positions[arrive_ID].y})
        distance += get_distance_hav(float(positions[depart_ID].y), float(positions[depart_ID].x),
                                     float(positions[arrive_ID].y), float(positions[arrive_ID].x))
    if len(list(person)) == 0:
        return False
    return True


r = shapefile.Reader(r"zipcodes/zipcode")
vehiclesWithinZipcode = eTree.Element("vehiclesWithinZipcode")
for rec in r.iterShapeRecords():
    ls = rec.record
    entry = eTree.SubElement(vehiclesWithinZipcode, "entry")
    zipcode = eTree.SubElement(entry, "zipcode")
    zipcode.text = ls['ZIP_CODE']
    number = eTree.SubElement(entry, "number")
    number.text = ls['EV']
tree = eTree.ElementTree(vehiclesWithinZipcode)
tree.write('xmlFiles/vehiclesWithinZipcode.xml', encoding="utf-8", xml_declaration=True)

growth_data = {}
data_list = []
with open('xmlFiles/EIA_national_BEV_projection.csv', 'r', encoding='utf-8') as file:
    reader = csv.reader(file, dialect='excel')
    for item in reader:
        data_list.append(item)
    for i in range(len(data_list[0])):
        if i == 0:
            continue
        growth_data[data_list[0][i]] = float(data_list[1][i]) + float(data_list[2][i])

projections = eTree.Element("projections")
for year_str in growth_data:
    year = int(year_str) + 1
    if str(year) in growth_data:
        entry = eTree.SubElement(projections, "entry")
        year = eTree.SubElement(entry, "year")
        year.text = str(int(year_str) + 1)
        growth = eTree.SubElement(entry, "growth")
        current_year = float(growth_data[year_str])
        next_year = float(growth_data[str(int(year_str) + 1)])
        growth.text = str(next_year / current_year)
tree = eTree.ElementTree(projections)
tree.write('xmlFiles/projections.xml', encoding="utf-8", xml_declaration=True)

household_position = {}
household_tract = {}
with open('xmlFiles/households.csv', 'r', encoding='utf-8') as file:
    reader = csv.reader(file, dialect='excel')
    for item in reader:
        household_position[item[5]] = item[7]
        household_tract[item[5]] = positions[item[7]].tract

r = shapefile.Reader(r"tracts/tract")
tract_Names = set()
for rec in r.iterShapeRecords():
    ls = rec.record
    tract_Names.add(ls.NAME)

# initializing every person's detail data task, it is a map, key is household + person number + day number
# value is person with tasks
persons = eTree.Element("persons")
begin_date = "2020-02-13"
next_date = "2020-02-14"
item_list = []
counter = 0
a = 0
with open('xmlFiles/persons.csv', 'r', encoding='utf-8') as file:
    reader = csv.reader(file, dialect='excel')
    item = next(reader)
    person_ID = item[2] + item[3] + item[4]
    household_ID = item[2]
    for next_item in reader:
        next_person_ID = next_item[2] + next_item[3] + next_item[4]
        next_household_ID = next_item[2]
        if person_ID != next_person_ID:
            # a = a + 1
            # if a == 50:
            #     break
            entry = eTree.SubElement(persons, "entry")
            position_ID = household_position[household_ID]
            zipcode = positions[position_ID].zipcode
            tract = positions[position_ID].tract

            convert_result = entry_convert(item_list, person_ID, entry, zipcode, tract)
            if not convert_result:
                persons.remove(entry)
            person_ID = next_person_ID
            household_ID = next_household_ID
            item_list.clear()
        item = next_item
        item_list.append(item)
tree = eTree.ElementTree(persons)
tree.write('xmlFiles/persons.xml', encoding="utf-8", xml_declaration=True)

# for test
pass

# initializing the fueling stations data, the result is map, key is unique ID related task content in persons data
# value is tractID latitude longitude
styles = ["L1", "L2", "L3", "GASOLINE", "DIESEL", "HYDROGEN"]
fuelingStations = eTree.Element("fuelingStations")
with open('xmlFiles/fuelingStations.csv', 'r', encoding='utf-8') as file:
    reader = csv.reader(file, dialect='excel')
    for item in reader:
        entry = eTree.SubElement(fuelingStations, "entry")
        ID = eTree.SubElement(entry, "ID")
        ID.text = item[9]
        fuelingStation = eTree.SubElement(entry, "fuelingStation")
        coord = eTree.SubElement(fuelingStation, "coord", {"latitude": item[7], "longitude": item[8]})

        fuelingStyles = eTree.SubElement(fuelingStation, "originalFuelingStyles")

        for i in range(6):
            if item[i] != "":
                entry = eTree.SubElement(fuelingStyles, "entry")
                ID = eTree.SubElement(entry, "ID")
                ID.text = styles[i]
                interfaceNumber = eTree.SubElement(entry, "interfaceNumber")
                interfaceNumber.text = item[i + 1]

# ET.dump(population)
tree = eTree.ElementTree(fuelingStations)
tree.write('xmlFiles/fuelingStations.xml', encoding="utf-8", xml_declaration=True)

# initializing the fueling stations data, the result is map, key is unique ID related task content in persons data
# value is tractID latitude longitude
models = eTree.Element("models")
with open('xmlFiles/Models.csv', 'r', encoding='utf-8') as file:
    reader = csv.reader(file, dialect='excel')
    for item in reader:
        entry = eTree.SubElement(models, "entry")
        ID = eTree.SubElement(entry, "ID")
        ID.text = item[0] + item[1] + item[2]
        model = eTree.SubElement(entry, "model")
        vehicleType = eTree.SubElement(model, "vehicleType")
        vehicleType.text = item[3]
        if vehicleType.text == "BEV" or vehicleType.text == "PHEV":
            batteryCapacity = eTree.SubElement(model, "batteryCapacity")
            num = eTree.SubElement(batteryCapacity, "num")
            num.text = item[4]
            unit = eTree.SubElement(batteryCapacity, "unit")
            unit.text = "KILOWATTHOUR"

            MPKWH = eTree.SubElement(model, "MPKWH")
            num = eTree.SubElement(MPKWH, "num")
            num.text = item[5]
            unit = eTree.SubElement(MPKWH, "unit")
            unit.text = "MPKWH"

            elecFuelingStyles = eTree.SubElement(model, "elecFuelingStyles")
            styles = item[8].split()
            for style in styles:
                FuelingStyle = eTree.SubElement(elecFuelingStyles, "FuelingStyle")
                FuelingStyle.text = style

        if vehicleType.text == "FCEV" or vehicleType.text == "PHEV":
            tankCapacity = eTree.SubElement(model, "tankCapacity")
            num = eTree.SubElement(tankCapacity, "num")
            num.text = item[6]
            unit = eTree.SubElement(tankCapacity, "unit")
            unit.text = "GALLON"

            MPG = eTree.SubElement(model, "MPG")
            num = eTree.SubElement(MPG, "num")
            num.text = item[7]
            unit = eTree.SubElement(MPG, "unit")
            unit.text = "MPG"

            liquidFuelingStyles = eTree.SubElement(model, "liquidFuelingStyles")
            styles = item[9].split()
            for style in styles:
                FuelingStyle = eTree.SubElement(liquidFuelingStyles, "FuelingStyle")
                FuelingStyle.text = item[9]

        marketShare = eTree.SubElement(model, "marketShare")
        marketShare.text = item[10]

# ET.dump(population)
tree = eTree.ElementTree(models)
tree.write('xmlFiles/models.xml', encoding="utf-8", xml_declaration=True)
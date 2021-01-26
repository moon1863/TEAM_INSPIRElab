import csv
import shapefile
import pickle

from shapely.geometry import shape, Point, Polygon


class Position:
    def __init__(self, x, y, zipcode, tract):
        self.x = x
        self.y = y
        self.zipcode = zipcode
        self.tract = tract


shp = shapefile.Reader('zipcodes/zipcode')  # open the shapefile
feature_length = len(shp)
polygons = []
for i in range(feature_length):
    polygons.append(Polygon(shp.shape(i).points))
# all_shapes = shp.shapes()  # get all the polygons
# all_records = shp.records()
positions = {}
count = 0
missed_count = 0
with open('xmlFiles/positions.csv', 'r', encoding='utf-8') as file:
    reader = csv.reader(file, dialect='excel')
    item = next(reader)
    for item in reader:
        count = count + 1
        if count % 1000 == 0:
            print("has finished" + str(count))
            print("missed " + str(missed_count))
        pt = Point(float(item[6]), float(item[7]))  # an x,y tuple
        dist_min = 0
        dist_min_index = 0
        for i in range(feature_length):
            dist_this = pt.distance(polygons[i])
            if dist_this == 0:
                positions[item[2]] = Position(str(pt.x), str(pt.y), shp.record(i)['ZIP_CODE'], item[5])
                break
            if dist_this <= dist_min:
                dist_min = dist_this
                dist_min_index = i
        else:
            positions[item[2]] = Position(str(pt.x), str(pt.y), shp.record(dist_min_index)['ZIP_CODE'], item[5])
    file = open('xmlFiles/positions.pickle', 'wb')
    pickle.dump(positions, file)
    file.close()

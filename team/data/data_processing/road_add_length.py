import shapefile
from math import sin, asin, cos, radians, fabs, sqrt

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


def cal_length(points):
    length = 0
    point_number = len(points)
    for i in range(point_number - 1):
        lat0 = points[i][1]
        lng0 = points[i][0]
        lat1 = points[i + 1][1]
        lng1 = points[i + 1][0]
        length += get_distance_hav(lat0, lng0, lat1, lng1)
    return length


r = shapefile.Reader(r"roads/road")
w = shapefile.Writer(r"roads/road_with_distance", shapeType=r.shapeType)
w.fields = list(r.fields)
w.field("geoLenNum", "F", 15, 5)
w.field("geoLenUnit", "C")
w.field("isDirected", "L")
w.field("direction", "L")
w.field("maxSpeNum", "N")
w.field("maxSpeUnit", "C")

for rec in r.iterShapeRecords():
    ls = rec.record
    shp = rec.shape
    ls.append(cal_length(shp.points))
    ls.append("METER")
    ls.append(False)
    ls.append(False)
    ls.append(70)
    ls.append("MPH")
    w.record(*ls)
    w.shape(rec.shape)
w.close()

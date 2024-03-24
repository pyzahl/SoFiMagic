from datetime import datetime, timedelta

def get_date_time():
    return datetime.now()

def get_seconds_of_day():
    now = datetime.now()
    tz_offset = now.utcoffset().total_seconds() if now.utcoffset() else 0
    h = now.hour - int(tz_offset // 3600)
    m = now.minute
    s = now.second
    return h*3600 + m*60 + s

def get_milliseconds_of_day(system_time_offset):
    now = datetime.now()
    tz_offset = now.utcoffset().total_seconds() if now.utcoffset() else 0
    h = now.hour - int(tz_offset // 3600)
    m = now.minute
    s = now.second
    ms = now.microsecond // 1000
    return round((h*3600 + m*60 + s + system_time_offset)*1000 + ms)

def get_hmsms_from_ms(ms):
    abs_ms = int(abs(ms))
    HH = abs_ms // 3600000
    abs_ms -= HH * 3600000
    MM = abs_ms // 60000
    abs_ms -= MM * 60000
    SS = abs_ms // 1000
    abs_ms -= SS * 1000
    MS = abs_ms
    if ms >= 0:
        return "{:02d}:{:02d}:{:02d}.{:03d}".format(HH, MM, SS, MS)
    else:
        return "-{:02d}:{:02d}:{:02d}.{:03d}".format(HH, MM, SS, MS)

def get_hms_from_ms(ms):
    abs_ms = abs(ms)
    HH = abs_ms // 3600000
    abs_ms -= HH * 3600000
    MM = abs_ms // 60000
    abs_ms -= MM * 60000
    SS = abs_ms // 1000
    if ms >= 0:
        return "{:02d}:{:02d}:{:02d}".format(HH, MM, SS)
    else:
        return "-{:02d}:{:02d}:{:02d}".format(HH, MM, SS)



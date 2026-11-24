from datetime import datetime, timedelta, timezone

# Time base for all program timing and log time stamps:
# False (default): local system time, contact times in the XML are local time.
# True:            UTC, contact times in the XML are UTC (same as the Sony app).
USE_UTC = False

def set_use_utc(use_utc):
    global USE_UTC
    USE_UTC = bool(use_utc)

def get_use_utc():
    return USE_UTC

def _now():
    return datetime.now(timezone.utc) if USE_UTC else datetime.now()

def get_date_time():
    return _now()

def get_seconds_of_day():
    now = _now()
    h = now.hour
    m = now.minute
    s = now.second
    return h*3600 + m*60 + s

def get_milliseconds_of_day(system_time_offset):
    now = _now()
    h = now.hour
    m = now.minute
    s = now.second
    ms = now.microsecond // 1000
    return round((h*3600 + m*60 + s + system_time_offset)*1000 + ms) % 86400000 # wrap into 0..24h (offset can leave the day)

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



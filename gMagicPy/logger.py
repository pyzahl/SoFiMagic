import os
from datetime import datetime
from timeutil import *

class Logger:
    @staticmethod
    def get_file():
        return os.path.join(".", "LOG.TXT")

    level = 2
    system_time_offset = 0
    
    @staticmethod
    def log_time_ms(msg):
        try:
            # Assuming BaseActivity.getHMSMSfromMS(BaseActivity.getMilliSecondsOfDay()) is replaced with a Python equivalent
            date = get_hmsms_from_ms(get_milliseconds_of_day(system_time_offset))  #    datetime.now().strftime("%H:%M:%S.%f")[:-3]
            os.makedirs(os.path.dirname(Logger.get_file()), exist_ok=True)
            print(f"{date} {msg}\n")
            with open(Logger.get_file(), "a") as writer:
                writer.write(f"{date} {msg}\n")
        except IOError:
            pass

    @staticmethod
    def log(msg):
        try:
            date=get_hmsms_from_ms(get_milliseconds_of_day(system_time_offset))  # date = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            #ms = datetime.now().microsecond // 1000
            os.makedirs(os.path.dirname(Logger.get_file()), exist_ok=True)
            #print(f"{date}.{ms:03d} {msg}\n")
            print(f"{date} {msg}\n")
            with open(Logger.get_file(), "a") as writer:
                #writer.write(f"{date}.{ms:03d} {msg}\n")
                writer.write(f"{date} {msg}\n")
        except IOError:
            pass

    @staticmethod
    def set_system_time_offset(dt):
        Logger.sysetm_time_offset = dt

    @staticmethod
    def set_verbose_level(l):
        Logger.level = l

    @staticmethod
    def get_verbose_level():
        return Logger.level

    #@staticmethod
    def log_type(L, type, msg):
        if Logger.level > L:
            Logger.log(f"[{type}] {msg}")

    @staticmethod
    def info_debug(msg):
        Logger.log_type(2, "DEBUG", msg)

    @staticmethod
    def info(msg):
        Logger.log_type(1, "INFO", msg)

    @staticmethod
    def info_progress(msg):
        Logger.log_type(0, "INFO", msg)

    @staticmethod
    def shootdata(phase, msg):
        Logger.log_type(-1, f"PHOTO of {phase}", msg)

    @staticmethod
    def error(msg):
        Logger.log_type(-1, "ERROR", msg)



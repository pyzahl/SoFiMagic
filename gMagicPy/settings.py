from logger import *
from settingsXML import *

class Settings:
    tc1 = 12*3600
    tc2 = tc1+3600
    tc3 = tc2+4*60
    tc4 = tc3+3600

    @staticmethod
    def set_contact_times(stc1, stc2, stc3, stc4):
        Settings.tc1 = Settings.parseHMStoSec(stc1)
        Settings.tc2 = Settings.parseHMStoSec(stc2)
        Settings.tc3 = Settings.parseHMStoSec(stc3)
        Settings.tc4 = Settings.parseHMStoSec(stc4)

    @staticmethod
    def parseHMStoSec(tc):
        hms = tc.split(":")
        h = int(hms[0])
        m = int(hms[1])
        s = int(hms[2])
        return h*3600+m*60+s

    @staticmethod
    def setVerboseLevel(level):
        Logger.set_verbose_level(int(level))

    MAX_EXPOSURE_PARAMS = 32

    class shoot_program:
        def __init__(self, name, ref_ci, ref_cf, ti, tf, number_shots):
            self.name = name
            self.ref_contact_start = ref_ci
            self.ref_contact_end = ref_cf
            self.start_time = ti
            self.end_time = tf
            self.number_shots = number_shots
            self.CameraFlags = ['S'] * Settings.MAX_EXPOSURE_PARAMS
            self.Fs = [0.0] * Settings.MAX_EXPOSURE_PARAMS
            self.BurstDurations = [0] * Settings.MAX_EXPOSURE_PARAMS
            self.ISOs = [0] * Settings.MAX_EXPOSURE_PARAMS
            self.ShutterSpeedsN = [0] * Settings.MAX_EXPOSURE_PARAMS
            self.ShutterSpeedsD = [0] * Settings.MAX_EXPOSURE_PARAMS

        skip = False

        def get_CFs_list(self):
            cf_list = ""
            for k in range(len(self.CameraFlags)):
                if self.ISOs[k] > 0:
                    cf_list += str(self.CameraFlags[k]) + ","
                else:
                    break
            return cf_list

        def get_ISOs_list(self):
            ISO_list = ""
            for k in range(len(self.ISOs)):
                if self.ISOs[k] > 0:
                    ISO_list += str(self.ISOs[k]) + ","
                else:
                    break
            return ISO_list

        def get_BurstDurations_list(self):
            BC_list = ""
            for k in range(len(self.BurstDurations)):
                if self.ISOs[k] > 0:
                    BC_list += str(self.BurstDurations[k]) + ","
                else:
                    break
            return BC_list

        def get_Fs_list(self):
            F_list = ""
            for k in range(len(self.Fs)):
                if self.ISOs[k] > 0:
                    F_list += str(self.Fs[k]) + ","
                else:
                    break
            return F_list

        def get_ShutterSpeeds_list(self):
            SHUTTER_list = ""
            for k in range(len(self.ShutterSpeeds)):
                if self.ISOs[k] > 0:
                    SHUTTER_list += str(self.ShutterSpeedsN[k]) + "/" + str(self.ShutterSpeedsD[k]) + ","
                else:
                    break
            return SHUTTER_list

        def set_CFs_list(self, CFs_list):
            cf_list = CFs_list.split(",")
            k = 0
            for cf in cf_list:
                self.CameraFlags[k] = cf
                k=k+1
            for k in range(k, len(self.CameraFlags)):
                self.CameraFlags[k] = 'S'
            print ('CFs: ', self.CameraFlags)

        def set_ISOs_list(self, ISO_list):
            iso_list = ISO_list.split(",")
            k=0
            for iso in iso_list:
                if iso != '':
                    self.ISOs[k] = int(iso)
                    k=k+1
                else:
                    break
            for i in range(k, len(self.ISOs)):
                self.ISOs[i] = 0

            print ('ISOs: ',self.ISOs)

        def set_BurstDurations_list(self, BC_list):
            bc_list = BC_list.split(",")
            k = 0
            for bc in bc_list:
                if bc != '' and self.ISOs[k] > 0:
                    self.BurstDurations[k] = int(bc_list[k])
                    k=k+1
                else:
                    break
            for k in range(k, len(self.BurstDurations)):
                self.BurstDurations[k] = 0
            print ('BDs: ', self.BurstDurations)

        def set_F_list(self, F_list):
            f_list = F_list.split(",")
            k = 0
            for f in f_list:
                if f != '' and self.ISOs[k] > 0:
                    self.Fs[k] = float(f_list[k])
                    k=k+1
            print ('Fs: ', self.Fs)

        def set_SHUTTER_SPEEDS_list(self, SHUTTER_SPEED_list):
            ss_list = SHUTTER_SPEED_list.split(",")
            k = 0
            for ss in ss_list:
                if ss != '' and self.ISOs[k] > 0:
                    snd = ss.split("/")
                    self.ShutterSpeedsN[k] = int(snd[0])
                    self.ShutterSpeedsD[k] = int(snd[1])
                    k=k+1
            print ('SSs: 1/..', self.ShutterSpeedsD)

        def get_start_time(self):
            return Settings.get_TC(self.ref_contact_start) + self.start_time

        def get_end_time(self):
            return Settings.get_TC(self.ref_contact_end) + self.end_time

        def get_remainingTimeToStart(self, as_of_ms):
            return round(self.get_start_time() * 1000.0 - as_of_ms)

        def get_remainingTime(self, as_of_ms):
            return round(self.get_end_time() * 1000.0 - as_of_ms)

        def get_TimeOfNext(self, count):
            if self.number_shots > 0:
                return self.get_start_time() + count*(self.get_end_time()-self.get_start_time())/(self.number_shots-1)
            else:
                return self.get_start_time()

        def get_remainingTimeToNext(self, count, as_of_ms):
            return round(self.get_TimeOfNext(count) * 1000.0 - as_of_ms)

    @staticmethod
    def get_TC(i):
        if i == 0:
            return (Settings.tc2+Settings.tc3)//2
        elif i == 1:
            return Settings.tc1
        elif i == 2:
            return Settings.tc2
        elif i == 3:
            return Settings.tc3
        elif i == 4:
            return Settings.tc4
        return -1

    magic_program = [None] * 17

    def __init__(self):
        Logger.log("Settings Init Magic Program")
        Settings.magic_program = [None] * 17

        Settings.tc1 = 3600*12
        Settings.tc2 = Settings.tc1+60*60+10*60
        Settings.tc3 = Settings.tc2+4*60
        Settings.tc4 = Settings.tc3+60*60+10*60

        phase = 0

        Settings.magic_program[phase] = Settings.shoot_program("Partial1", 1, 2, -30, -10, 100)
        P1PartialBursts = [0, 0, 0, 0]
        P1PartialISOs = [400, 400, 320, 0]
        P1PartialShutterSpeeds = [[1, 3200], [1, 2000], [1, 3200], [0, 0]]
        for i in range(len(P1PartialISOs)):
            Settings.magic_program[phase].CameraFlags[i] = 'S'
            Settings.magic_program[phase].BurstDurations[i] = P1PartialBursts[i]
            Settings.magic_program[phase].ISOs[i] = P1PartialISOs[i]
            Settings.magic_program[phase].Fs[i] = 0
            Settings.magic_program[phase].ShutterSpeedsN[i] = P1PartialShutterSpeeds[i][0]
            Settings.magic_program[phase].ShutterSpeedsD[i] = P1PartialShutterSpeeds[i][1]
        phase += 1

        Settings.magic_program[phase] = Settings.shoot_program("Contact2", 2, 2, -6, +6, -1)
        C2ShootingBursts = [12, 0]
        C2ShootingISOs = [100, 0]
        C2ShootingShutterSpeeds = [[1, 4000], [0, 0]]
        for i in range(len(C2ShootingISOs)):
            Settings.magic_program[phase].CameraFlags[i] = 'C'
            Settings.magic_program[phase].BurstDurations[i] = C2ShootingBursts[i]
            Settings.magic_program[phase].ISOs[i] = C2ShootingISOs[i]
            Settings.magic_program[phase].Fs[i] = 0
            Settings.magic_program[phase].ShutterSpeedsN[i] = C2ShootingShutterSpeeds[i][0]
            Settings.magic_program[phase].ShutterSpeedsD[i] = C2ShootingShutterSpeeds[i][1]
        phase += 1

        Settings.magic_program[phase] = Settings.shoot_program("TotalityA", 2, 0, +6, -30, -1)
        TotalityABursts = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        TotalityAISOs = [50, 100, 400, 800, 800, 800, 800, 800, 800, 800, 800, 0]
        TotalityAShutterSpeeds = [[1, 4000], [1, 2000], [1, 1000], [1, 1000], [1, 500], [1, 250], [1, 100], [1, 50], [1, 20], [1, 4], [1, 1], [0, 0]]
        for i in range(len(TotalityAISOs)):
            Settings.magic_program[phase].CameraFlags[i] = 'S'
            Settings.magic_program[phase].BurstDurations[i] = TotalityABursts[i]
            Settings.magic_program[phase].ISOs[i] = TotalityAISOs[i]
            Settings.magic_program[phase].Fs[i] = 0
            Settings.magic_program[phase].ShutterSpeedsN[i] = TotalityAShutterSpeeds[i][0]
            Settings.magic_program[phase].ShutterSpeedsD[i] = TotalityAShutterSpeeds[i][1]
        phase += 1

        Settings.magic_program[phase] = Settings.shoot_program("MaxTotality", 0, 0, -30, +30, -1)
        MaxTotalityBursts = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        MaxTotalityISOs = [100, 400, 800, 800, 800, 800, 800, 800, 800, 800, 800, 800, 0]
        MaxTotalityShutterSpeeds = [[1, 1000], [1, 1000], [1, 1000], [1, 500], [1, 1000], [1, 500], [1, 1000], [1, 500], [1, 100], [1, 20], [1, 1], [2, 1], [0, 0]]
        for i in range(len(MaxTotalityISOs)):
            Settings.magic_program[phase].CameraFlags[i] = 'S'
            Settings.magic_program[phase].BurstDurations[i] = MaxTotalityBursts[i]
            Settings.magic_program[phase].ISOs[i] = MaxTotalityISOs[i]
            Settings.magic_program[phase].Fs[i] = 0
            Settings.magic_program[phase].ShutterSpeedsN[i] = MaxTotalityShutterSpeeds[i][0]
            Settings.magic_program[phase].ShutterSpeedsD[i] = MaxTotalityShutterSpeeds[i][1]
        phase += 1

        Settings.magic_program[phase] = Settings.shoot_program("TotalityB", 0, 3, +30, -6, -1)
        TotalityBBursts = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        TotalityBISOs = [50, 100, 400, 800, 800, 800, 800, 800, 800, 800, 800, 0]
        TotalityBShutterSpeeds = [[1, 4000], [1, 2000], [1, 1000], [1, 1000], [1, 500], [1, 250], [1, 100], [1, 50], [1, 20], [1, 4], [1, 1], [0, 0]]
        for i in range(len(TotalityBISOs)):
            Settings.magic_program[phase].CameraFlags[i] = 'S'
            Settings.magic_program[phase].BurstDurations[i] = TotalityBBursts[i]
            Settings.magic_program[phase].ISOs[i] = TotalityBISOs[i]
            Settings.magic_program[phase].Fs[i] = 0
            Settings.magic_program[phase].ShutterSpeedsN[i] = TotalityBShutterSpeeds[i][0]
            Settings.magic_program[phase].ShutterSpeedsD[i] = TotalityBShutterSpeeds[i][1]
        phase += 1

        Settings.magic_program[phase] = Settings.shoot_program("Contact3", 3, 3, -6, +6, -1)
        C3ShootingBursts = [12, 0]
        C3ShootingISOs = [100, 0]
        C3ShootingShutterSpeeds = [[1, 4000], [0, 0]]
        for i in range(len(C3ShootingISOs)):
            Settings.magic_program[phase].CameraFlags[i] = 'C'
            Settings.magic_program[phase].BurstDurations[i] = C3ShootingBursts[i]
            Settings.magic_program[phase].ISOs[i] = C3ShootingISOs[i]
            Settings.magic_program[phase].Fs[i] = 0
            Settings.magic_program[phase].ShutterSpeedsN[i] = C3ShootingShutterSpeeds[i][0]
            Settings.magic_program[phase].ShutterSpeedsD[i] = C3ShootingShutterSpeeds[i][1]
        phase += 1

        Settings.magic_program[phase] = Settings.shoot_program("Partial2", 3, 4, +10, +30, 100)
        P2PartialBursts = [0, 0, 0, 0]
        P2PartialISOs = [400, 400, 320, 0]
        P2PartialShutterSpeeds = [[1, 3200], [1, 2000], [1, 3200], [0, 0]]
        for i in range(len(P2PartialISOs)):
            Settings.magic_program[phase].CameraFlags[i] = 'S'
            Settings.magic_program[phase].BurstDurations[i] = P2PartialBursts[i]
            Settings.magic_program[phase].ISOs[i] = P2PartialISOs[i]
            Settings.magic_program[phase].Fs[i] = 0
            Settings.magic_program[phase].ShutterSpeedsN[i] = P2PartialShutterSpeeds[i][0]
            Settings.magic_program[phase].ShutterSpeedsD[i] = P2PartialShutterSpeeds[i][1]
        phase += 1

        for i in range(phase, len(Settings.magic_program)):
            Settings.magic_program[i] = Settings.shoot_program("END", 4, 4, +30, +30, 0)
        Settings.shotCount = 1
        Settings.displayOff = False
        Settings.silentShutter = True
        Settings.fps = 0
        Settings.brs = True
        Settings.mf = True

        sofixml = SoFiProgramXML(self)
    



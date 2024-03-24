#!/bin/python3
from contextlib import contextmanager
import logging
import gphoto2 as gp
import locale
import os
import subprocess
import sys
import time
from datetime import datetime
import subprocess
import re

import logger
from settings import *
from timeutil import *

#       gphoto2 --capture-image
#	gphoto2 --set-config /main/capturesettings/shutterspeed2="1/60"
#	gphoto2 --set-config /main/capturesettings/f-number="f/5.6"
#	gphoto2 --set-config /main/capturesettings/exposurecompensation="3"
#	gphoto2 --set-config /main/imgsettings/iso=10000

# Callback functions. These should have the function signatures shown
# (with an extra 'self' if they're class methods).

def cb_idle(context, data):
    print('cb_idle', data)

def cb_error(context, text, data):
    print('cb_error', text, data)

def cb_status(context, text, data):
    print('cb_status', text, data)

def cb_message(context, text, data):
    print('cb_message', text, data)

def cb_question(context, text, data):
    print('cb_question', text, data)
    return gp.GP_CONTEXT_FEEDBACK_OK

def cb_cancel(context, data):
    print('cb_cancel', data)
    return gp.GP_CONTEXT_FEEDBACK_OK

def cb_progress_start(context, target, text, data):
    print('cb_progress_start', target, text, data)
    return 123

def cb_progress_update(context, id_, current, data):
    print('cb_progress_update', id_, current, data)

def cb_progress_stop(context, id_, data):
    print('cb_progress_stop', id_, data)

# Using a Python contextmanager to ensure callbacks are deleted when the
# gphoto2 context is no longer required. This example uses every
# available callback. You probably don't need all of them.



def list_files(camera, context, path='/'):
    result = []
    # get files
    for name, value in camera.folder_list_files(path, context):
        result.append(os.path.join(path, name))
    # read folders
    folders = []
    for name, value in camera.folder_list_folders(path, context):
        folders.append(name)
    # recurse over subfolders
    for name in folders:
        result.extend(list_files(camera, context, os.path.join(path, name)))
    return result


def get_datetime(config, model):
    # CAMERA TIME
    print ('*** Checking Camera Clock ***')
    for name, fmt in (('datetime', '%Y-%m-%d %H:%M:%S'),
                      ('d034',     None)):
        now = datetime.now()
        OK, datetime_config = gp.gp_widget_get_child_by_name(config, name)
        if OK >= gp.GP_OK:
            widget_type = datetime_config.get_type()
            raw_value = datetime_config.get_value()
            if widget_type == gp.GP_WIDGET_DATE:
                camera_time = datetime.fromtimestamp(raw_value)
            else:
                if fmt:
                    camera_time = datetime.strptime(raw_value, fmt)
                else:
                    camera_time = datetime.utcfromtimestamp(float(raw_value))
            print('Camera clock:  ', camera_time.isoformat(' '))
            print('Computer clock:', now.isoformat(' '))
            err = now - camera_time
            if err.days < 0:
                err = -err
                lead_lag = 'ahead'
                print('Camera clock is ahead by',)
            else:
                lead_lag = 'behind'
            print('Camera cqqlock is %s by %d days and %d seconds' % (
                lead_lag, err.days, err.seconds))
            print ('Camera UNIX DateTime = ', camera_time, camera_time.strftime('%s.%f'))
            return camera_time
            #break
    else:
        print('Unknown date/time config item')
        return 0

def auto_update_system_time(camera_datetime):

    print ('*** Checking for NTP Service ***')
    process = subprocess.Popen(['timedatectl'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = process.communicate()
    #print(out)
    lines = str(out).split('\\n')
    #print (lines)
    print (lines[4])
    clk_id = time.CLOCK_REALTIME
    print ('System UNIX DateTime = ', time.clock_gettime(clk_id))
    #print ('System NOW', float(datetime.now().strftime('%s.%f')))
    synced = lines[4].split(':')[1].lstrip().rstrip().lower()
    clock_is_synced = synced == 'yes'
    print ('System Time correct? : ', synced, ' => ', clock_is_synced)
    
    if not clock_is_synced:
        try:
            print ('Attempting to adjust system time... (needs root)')
            camera_dtime=float(camera_datetime.strftime('%s.%f'))
            clk_id = time.CLOCK_REALTIME
            time.clock_settime(clk_id, camera_dtime) # THIS REQUIRES ROOT
            print ('Synced System Time from Camera. DateTime=', time.clock_gettime())
            return 0, clock_is_synced
        except:
            camera_dtime=float(camera_datetime.strftime('%s.%f'))
            print ('Can not adjust system time, need to run this as root.\nEvaluating time difference for correction.')
            system_sec = time.clock_gettime(clk_id)
            print ('System UNIX DateTime = ', system_sec)
            print ('Camera UNIX DateTime = ', camera_dtime)
            print ('System is ', camera_dtime-system_sec, 's behind.')
            return camera_dtime-system_sec, clock_is_synced
    print ('*** TIME CHECKED ***')
    return 0, clock_is_synced

    
def set_camera_datetime(config, model):
    if model == 'Canon EOS 100D':
        OK, date_config = gp.gp_widget_get_child_by_name(config, 'datetimeutc')
        if OK >= gp.GP_OK:
            now = int(time.time())
            date_config.set_value(now)
            return True
    OK, sync_config = gp.gp_widget_get_child_by_name(config, 'syncdatetime')
    if OK >= gp.GP_OK:
        sync_config.set_value(1)
        return True
    OK, date_config = gp.gp_widget_get_child_by_name(config, 'datetime')
    if OK >= gp.GP_OK:
        widget_type = date_config.get_type()
        if widget_type == gp.GP_WIDGET_DATE:
            now = int(time.time())
            date_config.set_value(now)
        else:
            now = time.strftime('%Y-%m-%d %H:%M:%S')
            date_config.set_value(now)
        return True
    return False

def wait_busy (camera):
    ev_name = {}
    for name in ('GP_EVENT_UNKNOWN', 'GP_EVENT_TIMEOUT', 'GP_EVENT_FILE_ADDED',
                 'GP_EVENT_FOLDER_ADDED', 'GP_EVENT_CAPTURE_COMPLETE'):
        ev_name[getattr(gp, name)] = name

    print ('Waiting...')
    while True:
        try:
            ev_type, ev_data = camera.wait_for_event(2000) # 2000
        except KeyboardInterrupt:
            break
        if not ev_data:
            ev_data = ''
        print(datetime.now().strftime('%H:%M:%S'), ev_name[ev_type], ev_data)
        if ev_name[ev_type] == 'GP_EVENT_CAPTURE_COMPLETE' or ev_name[ev_type] == 'GP_EVENT_TIMEOUT':
            break
            

def exec_phase (phase, camera, config, shutterspeed_config, fnumber_config, iso_config, trigger_only, simulate, system_time_correct):
    Logger.set_system_time_offset(system_time_correct)

    now =  get_milliseconds_of_day(system_time_correct)
    print ('CHECKING PHASE: ', phase.name)
    print ('TIME NOW......: ', get_hmsms_from_ms(now))
    print ('PHASE START...: ', get_hmsms_from_ms(phase.get_start_time()*1000), ' remaining time: ', get_hmsms_from_ms(phase.get_remainingTimeToStart(now)))
    print ('PHASE END.....: ', get_hmsms_from_ms(phase.get_end_time()*1000),   ' remaining time: ', get_hmsms_from_ms(phase.get_remainingTime(now)))
    print ('FIRST SHOT IN.: ', get_hmsms_from_ms(phase.get_TimeOfNext(0)))
    print ('SECOND SHOT IN: ', get_hmsms_from_ms(phase.get_remainingTimeToNext(1,now)))
    print ('INTERVAL TIME.: ', get_hmsms_from_ms(1000.*(phase.get_end_time()-phase.get_start_time())/(phase.number_shots-1)))

    WORK_DIR = 'PhotosPhase/'+phase.name

    if not os.path.exists(WORK_DIR):
        os.makedirs(WORK_DIR)
    template = os.path.join(WORK_DIR, 'frame%04d.nef')

    count=0

    if now > phase.get_end_time()*1000:
        print ('Skipping forward over', phase.name)
        print ('***')
        return;
    
    while now < (phase.get_start_time()*1000-100):
        now =  get_milliseconds_of_day(system_time_correct)
        print ('\r',get_hmsms_from_ms(now),' ** remaining time to ', phase.name, ': ', get_hmsms_from_ms(phase.get_remainingTimeToStart(now)), end='')
        time.sleep(0.1)

    print ('\r\r** STARTING **')
    print ('')
    intervall_shoot = phase.number_shots > 0
    num = phase.number_shots
    i = phase.number_shots-num
    while phase.get_remainingTimeToNext(i,get_milliseconds_of_day(system_time_correct)) < -500:
        print ('\r Exposure#', i, '/', phase.number_shots, ': skipping', end='')
        num=num-1
        i = phase.number_shots-num

    print ('')
    while phase.get_remainingTime(get_milliseconds_of_day(system_time_correct)) > 0:
        if intervall_shoot:
            print ('Timed Interval Exposure Block ', i, '/', phase.number_shots, ' for ', phase.name)

            # wait until next intervall?
            i = phase.number_shots-num
            while phase.get_remainingTimeToNext(i,get_milliseconds_of_day(system_time_correct)) > 150:
                print ('\rremaining time to next '+phase.name+' shot #',i, ' in ', get_hmsms_from_ms(phase.get_remainingTimeToNext(i, get_milliseconds_of_day(system_time_correct))), end='')
                time.sleep(0.1)
            num = num-1
            print ('')

        else:
            print ('Continous Exposure Blocks Shooting for ', phase.name)

        for cf,bd,iso,f,sn,sd in zip(phase.CameraFlags, phase.BurstDurations, phase.ISOs, phase.Fs, phase.ShutterSpeedsN, phase.ShutterSpeedsD):
            if iso > 0:
                sss='{}/{}'.format(sn,sd)
                if sd > 1:
                    shutters = '{}/{}'.format(sn,sd)
                else:
                    shutters = '{}'.format(sn) # seconds only
                shutterspeed_config.set_value(shutters);
                fns='f/{:.1f}'.format(f)
                if f>0:
                    fnumber_config.set_value('f/{:.1f}'.format(f))
                iss='{}'.format(iso)
                iso_config.set_value(iss)
                try:
                    camera.set_config(config)
                except:
                    print (sss, '=> ', shutters, iss, fns)
                    print ('Set camera error')
                    print ('Ignoreing...')
                time.sleep(0.2)
                if simulate:
                    #print (get_hmsms_from_ms(get_milliseconds_of_day(system_time_correct))+'SIM Sap Photo for ', phase.name, ' @ ', sss, fns, iss)
                    Logger.shootdata(phase.name, ' SIM Snap #{} for {} {}/{} @{} {} {} '.format(count, phase.name, i, phase.number_shots, sss, fns, iss))
                else:
                    if trigger_only:
                        #print (get_hmsms_from_ms(get_milliseconds_of_day(system_time_correct))+' Snap Photo for ', phase.name, ' @ ', sss, fns, iss)
                        Logger.shootdata(phase.name, ' @{} {} {} '.format(sss, fns, iss))
                        camera.trigger_capture()
                        #wait_busy (camera)
                        print (get_hmsms_from_ms(get_milliseconds_of_day(system_time_correct))+' Completed')
                    else:
                        #print ('Snap Photo+download for ', phase.name, ' @ ', sss, fns, iss)
                        Logger.shootdata(phase.name, ' @{} {} {} '.format(sss, fns, iss))
                        path = camera.capture(gp.GP_CAPTURE_IMAGE)
                        print('capture', path.folder + path.name)
                        #wait_busy (camera)
                        camera_file = camera.file_get(
                            path.folder, path.name, gp.GP_FILE_TYPE_NORMAL)
                        camera_file.save(template % count)
                        camera.file_delete(path.folder, path.name)
                        #time.sleep(1)
                count = count + 1
            else:
                break

            
def main():
     
    locale.setlocale(locale.LC_ALL, '')
    logging.basicConfig(
        format='%(levelname)s: %(name)s: %(message)s', level=logging.WARNING)
    #callback_obj = gp.check_result(gp.use_python_logging())
    camera = gp.Camera()

    print('Please connect and switch on your camera!')
    i=1
    while True:
        try:
            camera.init()
        except gp.GPhoto2Error as ex:
            if ex.code == gp.GP_ERROR_MODEL_NOT_FOUND:
                # no camera, try again in 2 seconds
                time.sleep(2)
                print('\rwaiting for camera to connect...',i,end='')
                i=i+1
                continue
            print('Error waiting for camera to connect.')
            raise
        # operation completed successfully so exit loop
        break


    #callback_obj = gp.check_result(gp.use_python_logging())
    #with context_with_callbacks() as context:
    if True:
    
        #context = gp.Context()
        #text = camera.get_summary(context)
        #print(str(text))

        #print('Capturing image')
        #file_path = camera.capture(gp.GP_CAPTURE_IMAGE)
        #print('Camera file path: {0}/{1}'.format(file_path.folder, file_path.name))

        # get configuration tree
        abilities = camera.get_abilities()
        config = camera.get_config()

        # check camera time
        dt = get_datetime(config, abilities.model)

        system_delta_sec, sync = auto_update_system_time(dt)
        
        # sync camera time to computer
        # find the date/time setting config item and set it
        if sync:
            print ('*** Trying tyo sync Camera Date Time to System ***')
            if set_camera_datetime(config, abilities.model):
                # apply the changed config
                camera.set_config(config)
            else:
                print('Could not set date & time')


        # find the capture target config item
        capture_target = gp.check_result(
            gp.gp_widget_get_child_by_name(config, 'capturetarget'))
        # print current setting
        value = gp.check_result(gp.gp_widget_get_value(capture_target))
        print('Current setting for Capture Target:', value)
        # print possible settings
        for n in range(gp.check_result(gp.gp_widget_count_choices(capture_target))):
            choice = gp.check_result(gp.gp_widget_get_choice(capture_target, n))
            #print('Choice:', n, choice)
            

        shutter_speed2 = gp.check_result(
            gp.gp_widget_get_child_by_name(config, 'shutterspeed2'))
        # print current setting
        value = gp.check_result(gp.gp_widget_get_value(shutter_speed2))
        print('Current setting for Shutter Speed:', value)
        # print possible settings
        shutter_speed_options = []
        for n in range(gp.check_result(gp.gp_widget_count_choices(shutter_speed2))):
            choice = gp.check_result(gp.gp_widget_get_choice(shutter_speed2, n))
            #print('Choice:', n, choice)
            shutter_speed_options.append((n,choice))
            

        f_number = gp.check_result(
            gp.gp_widget_get_child_by_name(config, 'f-number'))
        # print current setting
        value = gp.check_result(gp.gp_widget_get_value(f_number))
        print('Current setting for f-number:', value)
        # print possible settings
        f_number_options = []
        for n in range(gp.check_result(gp.gp_widget_count_choices(f_number))):
            choice = gp.check_result(gp.gp_widget_get_choice(f_number, n))
            #print('Choice:', n, choice)
            f_number_options.append((n,choice))

        iso_setting = gp.check_result(
            gp.gp_widget_get_child_by_name(config, 'iso'))
        # print current setting
        value = gp.check_result(gp.gp_widget_get_value(iso_setting))
        print('Current setting for ISO:', value)
        # print possible settings
        iso_options = []
        for n in range(gp.check_result(gp.gp_widget_count_choices(iso_setting))):
            choice = gp.check_result(gp.gp_widget_get_choice(iso_setting, n))
            #print('Choice:', n, choice)
            iso_options.append((n,choice))

        #print ('Shutter Speeds: ', shutter_speed_options)
        #print ('f-numbers: ', f_number_options)
        #print ('ISOs: ', iso_options)
            
        shutterspeed_config = config.get_child_by_name('shutterspeed2')
        shutterspeed = shutterspeed_config.get_value()
        print (shutterspeed)
        
        fnumber_config = config.get_child_by_name('f-number')
        fnumber = fnumber_config.get_value()
        print (fnumber)

        iso_config = config.get_child_by_name('iso')
        iso = iso_config.get_value()
        print (iso)

        imgtarget_config = config.get_child_by_name('capturetarget')
        imgtarget = imgtarget_config.get_value()
        print (imgtarget)

        trigger_only = True
        
        if trigger_only:
            imgtarget_config.set_value('Memory card')
        else:
            imgtarget_config.set_value('Internal RAM')

        shutterspeed_config.set_value('1/500')
        fnumber_config.set_value('f/4')
        iso_config.set_value('400')
        camera.set_config(config)

        imgtarget = imgtarget_config.get_value()
        shutterspeed = shutterspeed_config.get_value()
        fnumber = fnumber_config.get_value()
        iso = iso_config.get_value()

        print ('***')
        print (imgtarget)
        print (shutterspeed)
        print (fnumber)
        print (iso)


        if True:
            # process program
            program = Settings()
            for ph in program.magic_program:
                if ph.name != 'END':
                    exec_phase (ph, camera, config, shutterspeed_config, fnumber_config, iso_config, trigger_only, False, system_delta_sec)
                else:
                    break

        
        camera.exit()

    
if __name__ == "__main__":
    sys.exit(main())

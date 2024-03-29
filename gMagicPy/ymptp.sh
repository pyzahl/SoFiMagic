#!/bin/sh

## SD=/run/user/1000/gvfs/gphoto2:host=NIKON_NIKON_DSC_D7000_000003051469/store_00010001/DCIM/235D7000
SD=SD
exiftool $SD/*.NEF  | grep "Create Date" | awk '(NR%2) {print $5}' > T
exiftool $SD/*.NEF | grep "ISO Setting  " | awk '{print $4}' > ISO
exiftool $SD/*.NEF  | grep "^Exposure Time   " | awk '{print $4}' > E
exiftool $SD/*.NEF  | grep "^F Number" | awk '{print $4}' > F

grep PHOTO LOG.TXT > PHOTO
paste T ISO E F PHOTO > verify.txt

less verify.txt

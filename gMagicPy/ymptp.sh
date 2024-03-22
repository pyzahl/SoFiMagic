#!/bin/sh

SD=/run/user/1000/gvfs/gphoto2:host=NIKON_NIKON_DSC_D7000_000003051469/store_00010001/DCIM/235D7000
exiftool $SD/DSC_31*.NEF  | grep "Create Date" | awk '(NR%2) {print $5}' > T
exiftool $SD/DSC_31*.NEF | grep "ISO Setting  " | awk '{print $4}' > ISO
exiftool $SD/DSC_31*.NEF  | grep "^Exposure Time   " | awk '{print $4}' > E

#grep PHOTO $SD/SOFIMAGI/LOG.TXT > PHOTO
#cp $SD/SOFIMAGI/LOG.TXT .

paste T ISO E > verify.txt
less verify.txt

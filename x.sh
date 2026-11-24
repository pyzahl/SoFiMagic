#!/bin/sh
exif /media/pzahl/disk/DCIM/100MSDCF/*JPG  | grep "Date and Time    " | awk '(NR%2) {print $5}' | sed 's/|//' > T
exif /media/pzahl/disk/DCIM/100MSDCF/*JPG  | grep "ISO" | awk '{print $4}'  | sed 's/|//' > ISO
exif /media/pzahl/disk/DCIM/100MSDCF/*JPG  | grep "Exposure Time" | awk '{print $3}'  | sed 's/|//' > E

grep PHOTO '/media/pzahl/disk/SOFIMAGI/LOG.TXT' > PHOTO
cp '/media/pzahl/disk/SOFIMAGI/LOG.TXT' .

paste T ISO E PHOTO | less

fastboot flash bootloader ./bootloader.img
fastboot reboot bootloader
fastboot flash radio ./radio.img
fastboot reboot bootloader
fastboot -w update ./1428.zip


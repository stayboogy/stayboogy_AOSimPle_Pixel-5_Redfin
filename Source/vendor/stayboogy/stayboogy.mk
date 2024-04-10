# stayboogy

PRODUCT_PRODUCT_PROPERTIES +=\
	ro.build.version.custom=v1.3.RC.4.04.24-stayboogy-perf

# Custom Apps
PRODUCT_PACKAGES += \
	ThemePicker \
	ExactCalculator \
	Recorder \
	Eleven \
	Etar \
	Jelly

# GrapheneOS SetupWizard
#PRODUCT_PACKAGES += \
#    	SetupWizard
	
# AuroraStore Prebuilt
include vendor/stayboogy/packages/apps/prebuilt/AuroraStore/Aurora.mk

# Custom Audio + Custom BootAnimation
include vendor/stayboogy/media/Media.mk

# CUTS

#PRODUCT_PACKAGES += \
#	SetupWizard 
	
# Black Theme from LineageOS
#PRODUCT_PACKAGES += \
#	BlackTheme

#BUILD_BROKEN_MISSING_REQUIRED_MODULES := true


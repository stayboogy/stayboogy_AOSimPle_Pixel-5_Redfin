include $(call first-makefiles-under,$(LOCAL_PATH))

PRODUCT_PACKAGES += \
    AuroraStore
    
PRODUCT_COPY_FILES := \
     vendor/stayboogy/packages/apps/prebuilt/AuroraStore/privapp-permissions-AuroraServices.xml:system_ext/etc/permissions/privapp-permissions-AuroraServices.xml


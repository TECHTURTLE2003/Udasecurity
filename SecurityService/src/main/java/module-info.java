module SecurityService {
    requires ImageService;
    requires java.datatransfer;
    requires java.desktop;
    requires java.prefs;
    requires miglayout;
    requires com.google.gson;
    requires com.google.common;
    opens com.udacity.catpoint.data to com.google.gson;
}
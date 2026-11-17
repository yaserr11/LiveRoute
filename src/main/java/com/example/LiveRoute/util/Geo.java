package com.example.LiveRoute.util;

/**
 * Cálculos sencillos con coordenadas GPS.
 */
public final class Geo {

    private static final double RADIO_TIERRA_METROS = 6_371_000;
    private static final String[] DIRECCIONES =
            {"norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste"};

    private Geo() {
    }

    /** Distancia en línea recta entre dos puntos (fórmula de haversine). */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_METROS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Dirección aproximada para ir del punto 1 al punto 2: "norte", "sureste"... */
    public static String direccion(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);
        double grados = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
        return DIRECCIONES[(int) Math.round(grados / 45) % 8];
    }
}

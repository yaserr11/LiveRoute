package com.example.LiveRoute.model;

import jakarta.persistence.*;

@Entity
@Table(name = "retos")
public class Reto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gincana_id")
    private Gincana gincana;

    private int orden;

    @Column(nullable = false, length = 500)
    private String pista;

    private String respuesta;

    private double latitud;
    private double longitud;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Gincana getGincana() { return gincana; }
    public void setGincana(Gincana gincana) { this.gincana = gincana; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public String getPista() { return pista; }
    public void setPista(String pista) { this.pista = pista; }

    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.dao;

/**
 *
 * @author Hasanjaya
 */
import com.example.model.Room;
import com.example.model.Sensor;
import com.example.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockDatabase {
    //Used to store rooms 
    public static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    //Used to store sensors
    public static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    //Used to store sensor readings
    public static final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.config;

import org.glassfish.jersey.server.ResourceConfig;
import javax.ws.rs.ApplicationPath;

/**
 * @author Hasanjaya Perera 20231509 (w2120668)
 * REST API Configuration
 */


@ApplicationPath("/api/v1")
//ResourceConfig inherits from javax.ws.rs.core.Application Beacuse of that used ResourceConfig
public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        //registering all resources
        packages("com.example.resource");
        packages("com.example.exception");
        packages("com.example.filter");
    }
}
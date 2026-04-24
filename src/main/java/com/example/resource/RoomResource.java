/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.resource;

/**
 *
 * @author Hasanjaya
 */
import com.example.dao.MockDatabase;
import com.example.exception.RoomNotEmptyException;
import com.example.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public Response getAllRooms() {
        return Response.ok(new ArrayList<>(MockDatabase.rooms.values())).build();
    }

    @POST
    public Response createRoom(Room room) {
        MockDatabase.rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = MockDatabase.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = MockDatabase.rooms.get(roomId);
        if (room != null) {
            if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
                throw new RoomNotEmptyException("Room cannot be deleted: It contains active sensors.");
            }
            MockDatabase.rooms.remove(roomId);
        }
        return Response.noContent().build();
    }
}

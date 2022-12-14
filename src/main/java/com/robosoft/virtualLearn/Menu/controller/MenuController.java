package com.robosoft.virtualLearn.Menu.controller;

import com.robosoft.virtualLearn.Menu.dto.NotificationResponse;
import com.robosoft.virtualLearn.Menu.dto.SideBarRequest;
import com.robosoft.virtualLearn.Menu.dto.SideBarResponse;
import com.robosoft.virtualLearn.Menu.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class MenuController {

    @Autowired
    MenuService menuService;


    @GetMapping("/Menu")
    public ResponseEntity<?> getSideBar(@RequestBody SideBarRequest request){
        SideBarResponse response = menuService.getUserDetails(request);
        if (response == null)
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        return ResponseEntity.of(Optional.of(response));
    }

    @GetMapping("/Notification")
    public List<NotificationResponse> getNotification(){
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        return menuService.getNotification(userName);
    }


}

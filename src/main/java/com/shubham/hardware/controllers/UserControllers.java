package com.shubham.hardware.controllers;

import com.shubham.hardware.dtos.ApiResponseMessage;
import com.shubham.hardware.dtos.UserDto;
import com.shubham.hardware.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shubham-hardware/users")
public class UserControllers {

    @Autowired
    private UserService userService;

//    create
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto){
        UserDto user = userService.createUser(userDto);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

//    update
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto,@PathVariable("userId") String id){
        UserDto updateUser = userService.updateUser(userDto,id);
        return new ResponseEntity<>(updateUser, HttpStatus.OK);
    }

//    delete
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponseMessage> deleteUser(@PathVariable("userId") String userId){
        userService.deleteUser(userId);
        ApiResponseMessage message = ApiResponseMessage.builder()
                .message("User is deleted successfully!!")
                .success(true)
                .status(HttpStatus.OK)
                .build();
        return new ResponseEntity<>(message,HttpStatus.OK);
    }

//    get single user
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId){
        UserDto user = userService.getUserById(userId);
        return new ResponseEntity<>(user,HttpStatus.OK);
    }

//    get all user
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(){
        List<UserDto> users= userService.getAllUsers();
        return new ResponseEntity<>(users,HttpStatus.OK);
    }

//    get user by email
    @GetMapping("/email/{userId}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable("userId") String userId){
        UserDto user = userService.getUserByEmail(userId);
        return new ResponseEntity<>(user,HttpStatus.OK);
    }

//    search user
    @GetMapping("/search/{keyword}")
    public ResponseEntity<List<UserDto>> searchUsers(@PathVariable("keyword") String keyword){
        List<UserDto> users= userService.searchUser(keyword);
        return new ResponseEntity<>(users,HttpStatus.OK);
    }
}
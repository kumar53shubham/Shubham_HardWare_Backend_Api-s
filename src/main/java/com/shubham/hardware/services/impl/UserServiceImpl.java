package com.shubham.hardware.services.impl;

import com.shubham.hardware.dtos.PageableResponse;
import com.shubham.hardware.dtos.UserDto;
import com.shubham.hardware.entities.Role;
import com.shubham.hardware.entities.User;
import com.shubham.hardware.exceptions.ResourceNotFoundException;
import com.shubham.hardware.helper.Helper;
import com.shubham.hardware.repo.RoleRepository;
import com.shubham.hardware.repo.UserRepository;
import com.shubham.hardware.services.UserService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "users")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    private Logger logger= LoggerFactory.getLogger(UserServiceImpl.class);

    @Value("${user.profile.image.path}")
    private String userImagePath;

    @Value("${admin.role.id}")
    private String adminRoleId;

    @Value("${normal.role.id}")
    private String normalRoleId;


    @Override
    @CachePut(key = "#userDto.userId")
    public UserDto createUser(UserDto userDto) {
        logger.info("UserService::createUser() connecting to database!!");
//        generate unique id in string format
        String userId = UUID.randomUUID().toString();
        userDto.setUserId(userId);

//        encoding password
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

//        dto-->entity
        User user = dtoToEntity(userDto);

//        fetch role of normal and set it to user
        Role role = roleRepository.findById(normalRoleId).get();
        user.getRoles().add(role);

        User savedUser = userRepository.save(user);
//        entity-->dto
        UserDto newDto = entityToDto(savedUser);
        return newDto;
    }


    @Override
    @CachePut(key = "#userId")
    public UserDto updateUser(UserDto userDto, String userId) {
        logger.info("UserService::updateUser() connecting to database!!");
        User user = userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found exception"));
        user.setName(userDto.getName());
//        we don't want to update the email
//        user.setEmail(userDto.getEmail());
        user.setAbout(userDto.getAbout());
        user.setImageName(userDto.getImageName());
        user.setGender(userDto.getGender());
//        encoding password the update
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        //update user
        User updatedUser = userRepository.save(user);
        UserDto updatedDto = entityToDto(updatedUser);
        return updatedDto;
    }

    @Override
    @CacheEvict(key = "#userId")
    public void deleteUser(String userId) {
        logger.info("UserService::deleteUser() connecting to database!!");
        User user = userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found exception"));
//        delete user image first before deleting user
        String imageName=user.getImageName();
        String fullPathWithImageName=userImagePath+imageName;// images/users/5a1012eb-fa97-4479-bcca-e5b66d70ef98.png
        logger.info("Full path with image name : {}",fullPathWithImageName);
        try {
            Path path= Paths.get(fullPathWithImageName);
            Files.delete(path);
        } catch (NoSuchFileException e) {
            logger.info("User image not found in folder!! : {}",e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        delete user
        user.setRoles(null);//removing the roles of user before deleting the user bcz user is dependent on roles (i.e, SqlConstraintViolationException)
        userRepository.delete(user);
    }

    @Override
    @Cacheable
    public UserDto getUserById(String userId) {
        logger.info("UserService::getUserById() connecting to database!!");
        User user = userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found"));
        UserDto userById = entityToDto(user);
        return userById;
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found with given email"));
        UserDto userDto = entityToDto(user);
        return userDto;
    }

    @Override
    @Cacheable
    public PageableResponse<UserDto> getAllUsers(int pageNumber, int pageSize, String sortBy, String sortDir) {
        logger.info("UserService::getAllUsers() connecting to database!!");
//        ternary operator
        Sort sort = (sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());
        Pageable pageable = PageRequest.of(pageNumber,pageSize,sort);
        Page<User> page = userRepository.findAll(pageable);

//        List<User> users = page.getContent();
////        using stream api
////        List<UserDto> dtoList = users.stream().map(user -> entityToDto(user)).collect(Collectors.toList());
//
////        using method reference
//        List<UserDto> dtoList = users.stream().map(this::entityToDto).toList();
//
//        PageableResponse<UserDto> response = new PageableResponse<>();
//        response.setContent(dtoList);
//        response.setPageNumber(page.getNumber());
//        response.setPageSize(page.getSize());
//        response.setTotalElements(page.getTotalElements());
//        response.setTotalPages(page.getTotalPages());
//        response.setLastPage(page.isLast());
//        return response;

//        Aliter
        PageableResponse<UserDto> response = Helper.getPageableResponse(page, UserDto.class);
        return response;
    }

    @Override
    public List<UserDto> searchUser(String keyword) {
        List<User> users = userRepository.findByNameContaining(keyword);
//        using stream api
        List<UserDto> dtoList = users.stream().map(user -> entityToDto(user)).collect(Collectors.toList());
        return dtoList;
    }

    @Override
    public UserDto assignUserAsAdmin(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with given id!!"));

//        assigning admin role to user
        Role role = roleRepository.findById(adminRoleId).get();
        user.getRoles().add(role);

        User adminUser = userRepository.save(user);
        UserDto adminUserDto = modelMapper.map(adminUser, UserDto.class);
        return adminUserDto;
    }

    @Override
    public Optional<User> findUserByEmailOptional(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user;
    }

    private User dtoToEntity(UserDto userDto) {
//        User user = User.builder()
//                .userId(userDto.getUserId())
//                .name(userDto.getName())
//                .email(userDto.getEmail())
//                .password(userDto.getPassword())
//                .about(userDto.getAbout())
//                .gender(userDto.getGender())
//                .imageName(userDto.getImageName())
//                .build();
//        return user;

        return modelMapper.map(userDto,User.class);

    }
    private UserDto entityToDto(User savedUser) {
//        UserDto user = UserDto.builder()
//                .userId(savedUser.getUserId())
//                .name(savedUser.getName())
//                .email(savedUser.getEmail())
//                .password(savedUser.getPassword())
//                .about(savedUser.getAbout())
//                .gender(savedUser.getGender())
//                .imageName(savedUser.getImageName())
//                .build();
//        return user;

        return modelMapper.map(savedUser, UserDto.class);
    }

}

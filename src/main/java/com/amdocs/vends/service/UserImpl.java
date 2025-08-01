package com.amdocs.vends.service;

import com.amdocs.vends.bean.Property;
import com.amdocs.vends.bean.Tenant;
import com.amdocs.vends.bean.User;
import com.amdocs.vends.dao.JDBC;
import com.amdocs.vends.interfaces.UserIntf;
import com.amdocs.vends.utils.enums.PropertyType;
import com.amdocs.vends.utils.enums.Role;
import com.amdocs.vends.utils.exceptions.DuplicateUsernameException;
import com.amdocs.vends.utils.singleton.LoggedInUser;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class UserImpl implements UserIntf {

    Scanner scanner = new Scanner(System.in);

    public void showAdminHomepage() {
        PropertyImpl propertyService = new PropertyImpl();
        TenantImpl tenantService = new TenantImpl();
        System.out.println("Welcome, Mr. " + LoggedInUser.getName());
        System.out.println();
        do {
            System.out.println("1. Add Property");
            System.out.println("2. Add Tenant");
            System.out.println("3. Show Properties and Tenants");
            System.out.println("4. Payment Approval Requests");
            System.out.println("5. Leave Approval Requests");
            System.out.println("6. Logout");
            System.out.print("Enter your choice between number 1 to 6: ");
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    Integer userId = LoggedInUser.getUserId();
                    String propertyType;
                    String address;
                    String city;
                    String state;
                    String status = "active";
                    System.out.println("Select Property Type:");
                    System.out.println("1. Villa    2. Flat    3. Plotted House");
                    System.out.print("Enter choice between 1-3: ");
                    int propertyTypeChoice = scanner.nextInt();
                    if (propertyTypeChoice == 1) {
                        propertyType = PropertyType.VILLA.getValue();
                    } else if (propertyTypeChoice == 2) {
                        propertyType = PropertyType.FLAT.getValue();
                    } else if (propertyTypeChoice == 3) {
                        propertyType = PropertyType.PLOTTED_HOUSE.getValue();
                    } else {
                        System.out.println("Wrong choice!");
                        break;
                    }
                    System.out.println("Enter Address: ");
                    scanner.nextLine();
                    address = scanner.nextLine();
                    System.out.println("Enter City: ");
                    city = scanner.nextLine();
                    System.out.println("Enter State: ");
                    state = scanner.nextLine();
                    Property property = new Property(userId, propertyType, address, city, state, status);
                    propertyService.addProperty(property);
                    System.out.println("Added property successfully!");
                    break;
                case 2:
                    userId = LoggedInUser.getUserId();
                    String name;
                    String username;
                    String role = Role.TENANT.getValue();
                    String passwordHash;
                    String phoneNumber;
                    Integer propertyId;
                    Float rent;
                    scanner.nextLine();
                    System.out.println("Enter name of tenant: ");
                    name = scanner.nextLine();
                    System.out.println("Enter username of tenant: ");
                    username = scanner.nextLine();
                    System.out.println("Enter temporary password of tenant: ");
                    passwordHash = scanner.nextLine();
                    System.out.println("Enter phone number of tenant: ");
                    phoneNumber = scanner.nextLine();
                    User user = new User(role, name, username, passwordHash, true, phoneNumber);
                    try {
                        userId = addUser(user);
                    } catch (DuplicateUsernameException e) {
                        showAdminHomepage();
                        System.err.println(e.getMessage());
                    }
                    Boolean isCurrentlyLivingThere = true;
                    System.out.println("Enter property Id: ");
                    propertyId = scanner.nextInt();
                    System.out.println("Enter rent: ");
                    rent = scanner.nextFloat();
                    Tenant tenant = new Tenant(userId, propertyId, rent, isCurrentlyLivingThere);
                    tenantService.addTenant(tenant);
                    System.out.println("Added tenant successfully!");
                    break;
                default: System.exit(0);
            }
        } while (true);
    }

    public void showHomepage() {
        do {
            System.out.println("1. SignUp");
            System.out.println("2. Login");
            System.out.println("3. Exit Program");
            System.out.print("Enter choice between 1-3: ");
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    break;
                case 2:
                    String username;
                    String password;
                    System.out.println("Enter username: ");
                    scanner.nextLine();
                    username = scanner.nextLine();
                    System.out.println("Enter password: ");
                    password = scanner.nextLine();
                    int successCode = login(username, password);
                    if (successCode == 1 && LoggedInUser.getRole().equals(Role.ADMIN)) {
                        showAdminHomepage();
                    } else {

                    }
                    break;
                case 3:
                    System.exit(0);
            }
        } while (true);
    }

    @Override
    public Integer addUser(User user) throws DuplicateUsernameException {
        try {
            Connection connection = JDBC.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM users WHERE username = '" + user.getUsername()+"'");
            if (resultSet.getFetchSize() > 0) {
                throw new DuplicateUsernameException("Username already exists!");
            }
            int result = statement.executeUpdate("INSERT INTO users (role, name, username, password_hash, first_login, phone_number) VALUES ('"+user.getRole()+"','"+user.getName()+"','"+user.getUsername()+"','"+user.getPasswordHash()+"',"+user.getFirstLogin()+",'"+user.getPhoneNumber()+"')");
            if (result == 0) {
                System.out.println("Failed to create user in database!");
            }
            resultSet = statement.executeQuery("SELECT MAX(id) FROM users");
            while(resultSet.next()) {
                return Integer.parseInt(resultSet.getString(1));
            }
        } catch (Exception e) {
            System.err.println("Failed to create user in database! Reason: "  + e.getMessage());
        }
        return -1;
    }

    @Override
    public int login(String username, String password) {
        try {
            Connection connection = JDBC.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM users WHERE username = '" + username + "'");
//            if (resultSet.getFetchSize() == 0) {
//                System.out.println("User with above username does not exist! Check credentials and try again");
//                return 0;
//            }
            while (resultSet.next()) {
                String storedPassword = resultSet.getString(5);
                if (!storedPassword.equals(password)) {
                    System.out.println("Invalid Password! Check credentials and try again");
                    return 0;
                }
                LoggedInUser.setUserId(Integer.parseInt(resultSet.getString(1)));
                LoggedInUser.setName(resultSet.getString(3));
                LoggedInUser.setRole(resultSet.getString(2).toUpperCase());
                return 1;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return 0;
    }
}

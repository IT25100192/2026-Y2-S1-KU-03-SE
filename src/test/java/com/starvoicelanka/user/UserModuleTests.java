package com.starvoicelanka.user;

import com.starvoicelanka.common.exception.BadRequestException;
import com.starvoicelanka.common.exception.ConflictException;
import com.starvoicelanka.user.dto.UserDtos;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.entity.UserStatus;
import com.starvoicelanka.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class UserModuleTests {

    @Autowired
    private UserService userService;

    @Test
    void testRegisterAndVerify() {
        UserDtos.RegisterRequest req = new UserDtos.RegisterRequest();
        req.setFullName("Test User");
        req.setEmail("testuser@example.com");
        req.setMobile("0779998877");
        req.setPassword("Password123!");

        User registered = userService.register(req);
        assertNotNull(registered.getId());
        assertEquals(UserStatus.PENDING, registered.getStatus());
        assertNotNull(registered.getVerificationCode());

        // Verify with code
        User verified = userService.verifyMobile("testuser@example.com", registered.getVerificationCode());
        assertEquals(UserStatus.ACTIVE, verified.getStatus());
        assertTrue(verified.isMobileVerified());
    }

    @Test
    void testDuplicateEmailFails() {
        UserDtos.RegisterRequest req = new UserDtos.RegisterRequest();
        req.setFullName("Admin Dupe");
        req.setEmail("admin@starvoice.lk"); // already in seed
        req.setMobile("0778889900");
        req.setPassword("Password123!");

        assertThrows(ConflictException.class, () -> userService.register(req));
    }

    @Test
    void testChangeRoleGuard() {
        User admin = userService.getUserByEmail("admin@starvoice.lk");
        assertThrows(com.starvoicelanka.common.exception.ForbiddenException.class, () -> userService.changeRole(admin.getId(), Role.VOTER, admin));
    }

    @Test
    void testDeleteUser() {
        User admin = userService.getUserByEmail("admin@starvoice.lk");
        User testVoter = userService.createUser("Deletable Voter", "deletable@test.com", "0771112233",
                "Password123!", Role.VOTER, UserStatus.ACTIVE, admin, "127.0.0.1");
        assertNotNull(testVoter.getId());

        userService.deleteUser(testVoter.getId(), admin, "127.0.0.1");

        assertThrows(com.starvoicelanka.common.exception.ResourceNotFoundException.class,
                () -> userService.getUserByEmail("deletable@test.com"));
    }
}

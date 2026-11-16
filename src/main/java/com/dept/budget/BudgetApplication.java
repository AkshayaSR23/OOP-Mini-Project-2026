package com.dept.budget;

import com.dept.budget.entity.Staff;
import com.dept.budget.repository.StaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Starts the app. Open http://localhost:8080 after it starts.
 */
@SpringBootApplication
public class BudgetApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(BudgetApplication.class, args);
    }

    /** Runs once at startup: creates the staff login if there is none yet. */
    @Bean
    CommandLineRunner createStaffLogin(StaffRepository staffRepository) {
        return args -> {
            if (staffRepository.count() == 0) {
                String password = System.getenv("STAFF_PASSWORD");
                if (password == null || password.isBlank()) {
                    password = "admin123";
                    System.out.println("NOTE: STAFF_PASSWORD is not set, so the login is budgetstaff / admin123."
                            + " Set STAFF_PASSWORD before real use.");
                }
                staffRepository.save(new Staff("budgetstaff", password));
            }
        };
    }

    /**
     * Security check on the server: every /api request must come from a logged-in staff,
     * except the login request itself.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("staffId") != null) {
                    return true;
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Please log in again\"}");
                return false;
            }
        }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login");
    }
}

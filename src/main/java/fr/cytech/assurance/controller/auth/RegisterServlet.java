package fr.cytech.assurance.controller.auth;

import fr.cytech.assurance.dto.request.RegisterRequest;
import fr.cytech.assurance.service.AuthService;
import fr.cytech.assurance.service.RegistrationException;
import fr.cytech.assurance.util.FormView;
import fr.cytech.assurance.util.RequestParams;
import fr.cytech.assurance.util.UserSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@WebServlet("/auth/register")
public class RegisterServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/auth/register.jsp";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{10,14}$");
    private static final int AGE_MINIMUM = 18;

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        String firstName = RequestParams.get(req, "firstName");
        String name = RequestParams.get(req, "name");
        String email = RequestParams.get(req, "email").toLowerCase();
        String phoneNumber = RequestParams.get(req, "phoneNumber").replace(" ", "");
        String birthDateText = RequestParams.get(req, "birthDate");
        String address = RequestParams.get(req, "address");
        String password = RequestParams.raw(req, "password");
        String confirmation = RequestParams.raw(req, "confirmation");

        Map<String, String> errors = new LinkedHashMap<>();

        if (firstName.isEmpty()) errors.put("firstName", "Le prénom est obligatoire.");
        else if (firstName.length() > 60) errors.put("firstName", "60 caractères maximum.");

        if (name.isEmpty()) errors.put("name", "Le nom est obligatoire.");
        else if (name.length() > 60) errors.put("name", "60 caractères maximum.");

        if (email.isEmpty()) errors.put("email", "L'adresse e-mail est obligatoire.");
        else if (email.length() > 120 || !EMAIL.matcher(email).matches())
            errors.put("email", "Adresse e-mail invalide.");

        if (!phoneNumber.isEmpty() && !PHONE.matcher(phoneNumber).matches())
            errors.put("phoneNumber", "Numéro invalide (10 à 14 chiffres).");

        LocalDate dateNaissance = null;
        if (birthDateText.isEmpty()) {
            errors.put("birthDate", "La date de naissance est obligatoire.");
        } else {
            try {
                dateNaissance = LocalDate.parse(birthDateText);
                if (dateNaissance.isAfter(LocalDate.now())) {
                    errors.put("birthDate", "La date ne peut pas être dans le futur.");
                } else if (Period.between(dateNaissance, LocalDate.now()).getYears() < AGE_MINIMUM) {
                    errors.put("birthDate", "Vous devez avoir au moins " + AGE_MINIMUM + " ans.");
                }
            } catch (DateTimeParseException e) {
                errors.put("birthDate", "Date invalide.");
            }
        }

        if (address.length() > 200) errors.put("address", "200 caractères maximum.");

        if (password.length() < 8 || !password.matches(".*\\d.*"))
            errors.put("password", "8 caractères minimum, dont un chiffre.");
        else if (password.length() > 72) // limite de BCrypt
            errors.put("password", "72 caractères maximum.");

        if (!password.equals(confirmation))
            errors.put("confirmation", "Les mots de passe ne correspondent pas.");

        if (!errors.isEmpty()) {
            FormView.show(req, resp, VIEW, errors);
            return;
        }

        RegisterRequest dto = new RegisterRequest(
                firstName,
                name,
                email,
                phoneNumber.isEmpty() ? null : phoneNumber,
                dateNaissance,
                address.isEmpty() ? null : address,
                password
        );

        try {
            authService.register(dto);
        } catch (RegistrationException e) {
            errors.put(e.getField(), e.getMessage());
            FormView.show(req, resp, VIEW, errors);
            return;
        } catch (RuntimeException e) {
            log("Erreur lors de l'inscription de " + email, e);
            FormView.show(req, resp, VIEW, errors, "Une erreur est survenue. Veuillez réessayer.");
            return;
        }

        // Post/Redirect/Get : message affiché par flash.jspf sur la page de connexion
        UserSession.flash(req, "Votre compte a été créé. Vous pouvez vous connecter.", "success");
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }
}

package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.request.RegisterRequest;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.service.exceptions.AuthException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.hibernate.exception.ConstraintViolationException;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9][0-9 .-]{5,19}$");
    private static final int MIN_AGE = 18;
    private static final String DUMMY_HASH = BCrypt.hashpw("mot-de-passe-factice", BCrypt.gensalt());
    private final UserDao userDao;
    private final RoleDao roleDao;

    public AuthService() {
        this(new UserDao(), new RoleDao());
    }

    public AuthService(UserDao userDao, RoleDao roleDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
    }

    public User register(RegisterRequest req) {
        Map<String, String> errors = validate(req);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String email = req.email().trim().toLowerCase();
        if (userDao.existsByEmail(email)) {
            throw new ValidationException(Map.of("email", "Cet email est déjà utilisé."));
        }

        Role clientRole = roleDao.findByName(Role.CLIENT)
                .orElseThrow(() -> new IllegalStateException("Le rôle CLIENT n'existe pas en base"));

        User user = new User(
                req.firstName().trim(),
                req.lastName().trim(),
                email,
                BCrypt.hashpw(req.password(), BCrypt.gensalt()),
                clientRole);
        user.setBirthDate(LocalDate.parse(req.birthDate().trim()));
        user.setPhone(blankToNull(req.phone()));
        user.setAddress(blankToNull(req.address()));

        try {
            return userDao.save(user);
        } catch (ConstraintViolationException e) {
            throw new ValidationException(Map.of("email", "Cet email est déjà utilisé."));
        }
    }

    private Map<String, String> validate(RegisterRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();

        String firstName = trim(req.firstName());
        if (firstName.isEmpty()) errors.put("prenom", "Le prénom est obligatoire.");
        else if (firstName.length() > 80) errors.put("prenom", "Le prénom est trop long (80 caractères max).");

        String lastName = trim(req.lastName());
        if (lastName.isEmpty()) errors.put("nom", "Le nom est obligatoire.");
        else if (lastName.length() > 80) errors.put("nom", "Le nom est trop long (80 caractères max).");

        String email = trim(req.email());
        if (email.isEmpty()) errors.put("email", "L'email est obligatoire.");
        else if (email.length() > 150 || !EMAIL.matcher(email).matches()) errors.put("email", "L'email n'est pas valide.");

        String phone = trim(req.phone());
        if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) errors.put("telephone", "Le numéro de téléphone n'est pas valide.");

        String address = trim(req.address());
        if (address.length() > 255) errors.put("adresse", "L'adresse est trop longue (255 caractères max).");

        String birth = trim(req.birthDate());
        if (birth.isEmpty()) {
            errors.put("dateNaissance", "La date de naissance est obligatoire.");
        } else {
            try {
                LocalDate date = LocalDate.parse(birth);
                if (date.isAfter(LocalDate.now().minusYears(MIN_AGE))) {
                    errors.put("dateNaissance", "Vous devez avoir au moins " + MIN_AGE + " ans.");
                } else if (date.isBefore(LocalDate.now().minusYears(120))) {
                    errors.put("dateNaissance", "La date de naissance n'est pas valide.");
                }
            } catch (DateTimeParseException e) {
                errors.put("dateNaissance", "La date de naissance n'est pas valide.");
            }
        }

        String password = req.password() == null ? "" : req.password();
        if (password.length() < 8) {
            errors.put("motDePasse", "Le mot de passe doit contenir au moins 8 caractères.");
        } else if (password.length() > 72) {                       // limite de BCrypt
            errors.put("motDePasse", "Le mot de passe est trop long (72 caractères max).");
        } else if (!password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*") || !password.matches(".*\\d.*")) {
            errors.put("motDePasse", "Le mot de passe doit contenir une minuscule, une majuscule et un chiffre.");
        }
        if (!password.equals(req.passwordConfirm())) {
            errors.put("confirmation", "Les deux mots de passe ne correspondent pas.");
        }

        return errors;
    }

    public User login(String email, String password) {
        email = email == null ? "" : email.trim().toLowerCase();
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new AuthException("Email ou mot de passe incorrect.");
        }

        Optional<User> found = userDao.findByEmail(email);
        String hash = found.map(User::getPassword).orElse(DUMMY_HASH);

        boolean passwordOk;
        try {
            passwordOk = BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            passwordOk = false;
        }

        if (found.isEmpty() || !passwordOk) {
            throw new AuthException("Email ou mot de passe incorrect.");
        }

        User user = found.get();
        if (!user.isActive()) {
            throw new AuthException("Ce compte est désactivé. Contactez un administrateur.");
        }
        return user;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

package assurcy2000.dto.request;

import java.time.LocalDate;

public class RegisterRequest {
    private String firstName;
    private String name;
    private String email;
    private String phoneNumber;
    private LocalDate birthDate;
    private String address;
    private String password;

    public RegisterRequest(String firstName, String name, String email, String phoneNumber, LocalDate birthDate, String address, String password){
        this.firstName = firstName;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.address = address;
        this.password = password;
    }

    public String getFirstName(){
        return firstName;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }
}

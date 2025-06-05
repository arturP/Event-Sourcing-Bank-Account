package io.artur.eventsourcing.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public class AccountHolder {
    
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-'.]{2,50}$");
    private final String firstName;
    private final String lastName;
    private final String fullName;
    
    public AccountHolder(String firstName, String lastName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        
        this.firstName = validateAndNormalizeName(firstName.trim());
        this.lastName = validateAndNormalizeName(lastName.trim());
        this.fullName = this.firstName + " " + this.lastName;
    }
    
    public AccountHolder(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        
        String normalizedFullName = fullName.trim();
        if (!NAME_PATTERN.matcher(normalizedFullName).matches()) {
            throw new IllegalArgumentException("Invalid name format");
        }
        
        String[] parts = normalizedFullName.split("\\s+", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Full name must contain at least first and last name");
        }
        
        this.firstName = parts[0];
        this.lastName = parts[1];
        this.fullName = normalizedFullName;
    }
    
    private String validateAndNormalizeName(String name) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name format: " + name);
        }
        return capitalizeWords(name);
    }
    
    private String capitalizeWords(String input) {
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)))
                      .append(words[i].substring(1));
            }
        }
        
        return result.toString();
    }
    
    public static AccountHolder of(String fullName) {
        return new AccountHolder(fullName);
    }
    
    public static AccountHolder of(String firstName, String lastName) {
        return new AccountHolder(firstName, lastName);
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getInitials() {
        return firstName.charAt(0) + "." + lastName.charAt(0) + ".";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountHolder that = (AccountHolder) o;
        return Objects.equals(fullName, that.fullName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fullName);
    }
    
    @Override
    public String toString() {
        return fullName;
    }
}
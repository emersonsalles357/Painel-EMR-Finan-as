package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.exception.RegraNegocioException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {
    public void validate(String password) {
        if (password == null || password.length() < 10) {
            throw new RegraNegocioException("A senha deve possuir pelo menos 10 caracteres");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new RegraNegocioException("A senha deve conter uma letra maiuscula");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new RegraNegocioException("A senha deve conter uma letra minuscula");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new RegraNegocioException("A senha deve conter um numero");
        }
        if (password.chars().noneMatch(character -> !Character.isLetterOrDigit(character)
                && !Character.isWhitespace(character))) {
            throw new RegraNegocioException("A senha deve conter um caractere especial");
        }
    }
}

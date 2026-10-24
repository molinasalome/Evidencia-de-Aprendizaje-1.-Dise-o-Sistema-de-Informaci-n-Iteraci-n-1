package com.autoscolombia.parqueadero.controlador;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Agrega a todas las pantallas el rol del usuario con sesión iniciada
@ControllerAdvice
public class AtributosGlobales {

    @ModelAttribute
    public void agregarRol(Authentication autenticacion, Model modelo) {
        if (autenticacion == null) {
            return;
        }
        boolean esAdmin = autenticacion.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        modelo.addAttribute("esAdmin", esAdmin);
        modelo.addAttribute("rolEtiqueta", esAdmin ? "Administrador" : "Vigilante");
    }
}
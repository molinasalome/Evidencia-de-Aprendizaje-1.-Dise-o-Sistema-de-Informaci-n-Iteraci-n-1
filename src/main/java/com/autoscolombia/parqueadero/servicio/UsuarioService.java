package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.MensualidadVencidaException;
import com.autoscolombia.parqueadero.excepcion.PlacaInvalidaException;
import com.autoscolombia.parqueadero.modelo.Cliente;
import com.autoscolombia.parqueadero.modelo.Vehiculo;
import com.autoscolombia.parqueadero.repositorio.MensualidadRepository;
import com.autoscolombia.parqueadero.repositorio.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final VehiculoRepository vehiculoRepository;
    private final MensualidadRepository mensualidadRepository;
    private final ValidacionPlacaService validacionPlacaService;

    public UsuarioService(VehiculoRepository vehiculoRepository,
                          MensualidadRepository mensualidadRepository,
                          ValidacionPlacaService validacionPlacaService) {
        this.vehiculoRepository = vehiculoRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.validacionPlacaService = validacionPlacaService;
    }

    // Devuelve el cliente dueño de la placa; si no existe, rechaza con el motivo
    public Cliente buscarPorPlaca(String placa) {
        String p = validacionPlacaService.normalizar(placa);
        Vehiculo vehiculo = vehiculoRepository.findByPlaca(p)
            .orElseThrow(() -> new PlacaInvalidaException(
                "La placa " + p + " no está registrada."));
        return vehiculo.getCliente();
    }

    public boolean tieneMensualidadVigente(String placa) {
        Cliente cliente = buscarPorPlaca(placa);
        return !mensualidadRepository
            .buscarVigentes(cliente.getIdUsuario(), LocalDate.now())
            .isEmpty();
    }

    // Autoriza o rechaza el ingreso: lanza excepción si algo falla
    public Cliente validarIngreso(String placa) {
        Cliente cliente = buscarPorPlaca(placa);
        if (!tieneMensualidadVigente(placa)) {
            throw new MensualidadVencidaException(
                "La mensualidad del usuario está vencida.");
        }
        return cliente;
    }
}
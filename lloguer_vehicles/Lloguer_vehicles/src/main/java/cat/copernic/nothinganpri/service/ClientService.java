
package cat.copernic.nothinganpri.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cat.copernic.nothinganpri.dto.DocumentClient;
import cat.copernic.nothinganpri.dto.DocumentacioForm;
import cat.copernic.nothinganpri.entity.ClientJpaEntity;
import cat.copernic.nothinganpri.repository.ClientRepository;
import cat.copernic.nothinganpri.repository.ReservaJpaRepository;

import jakarta.transaction.Transactional;

/**
 * Servicio que gestiona la lógica de negocio de los clientes.
 */
@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ReservaJpaRepository reservaRepository;

    @Autowired
    private LogQueryService logQueryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Obtiene todos los clientes activos.
     */
    public List<ClientJpaEntity> llistarTots() {
        try {
            return clientRepository.findByRolAndActivoTrue("CLIENT");
        } catch (RuntimeException e) {
            registrarError("llistarTots", e);
            throw e;
        }
    }

    /**
     * Convierte los valores vacíos o nulos en null.
     */
    private String buitANull(String valor) {
        return valor == null || valor.trim().isEmpty()
                ? null
                : valor.trim();
    }

    /**
     * Busca clientes aplicando diferentes filtros combinables.
     */
    public List<ClientJpaEntity> cercarFiltres(
            String nacionalitat,
            String provincia,
            String dni,
            String cognoms,
            String telefon,
            String email) {

        try {
            return clientRepository.cercarClients(
                    buitANull(nacionalitat),
                    buitANull(provincia),
                    buitANull(dni),
                    buitANull(cognoms),
                    buitANull(telefon),
                    buitANull(email),
                    "CLIENT");

        } catch (RuntimeException e) {
            registrarError("cercarFiltres", e);
            throw e;
        }
    }

    /**
     * Busca un cliente mediante su DNI.
     */
    public Optional<ClientJpaEntity> findByDNI(String DNI) {
        try {
            return clientRepository.findByDNIAndRol(DNI, "CLIENT");
        } catch (RuntimeException e) {
            registrarError("findByDNI", e);
            throw e;
        }
    }

    /**
     * Busca un cliente mediante su correo electrónico.
     */
    public Optional<ClientJpaEntity> findByEmail(String email) {
        try {
            return clientRepository.findByEmail(buitANull(email));
        } catch (RuntimeException e) {
            registrarError("findByEmail", e);
            throw e;
        }
    }

    /**
     * Busca un cliente mediante su identificador.
     */
    public Optional<ClientJpaEntity> findById(Long id) {
        try {
            return clientRepository.findById(id);
        } catch (RuntimeException e) {
            registrarError("findById", e);
            throw e;
        }
    }

    /**
     * Elimina definitivamente un cliente si no tiene reservas asociadas.
     */
    @Transactional
    public boolean eliminarClient(Long id) {

        try {
            Optional<ClientJpaEntity> client = clientRepository.findById(id);

            if (client.isEmpty() || !"CLIENT".equals(client.get().getRol())) {

                logQueryService.registrarWarning(
                        "CLIENTS",
                        "No se ha podido eliminar el cliente: no existe o no tiene el rol CLIENT.");

                return false;
            }

            boolean tieneReservas = reservaRepository.existsByClient_Id(id);

            if (tieneReservas) {

                logQueryService.registrarWarning(
                        "CLIENTS",
                        "No se ha podido eliminar el cliente porque tiene reservas asociadas. ID: " + id);

                return false;
            }

            clientRepository.delete(client.get());

            logQueryService.registrarInfo(
                    "CLIENTS",
                    "Cliente eliminado definitivamente. ID: " + id);

            return true;

        } catch (RuntimeException e) {
            registrarError("eliminarClient", e);
            throw e;
        }
    }

    /**
     * Obtiene la imagen del DNI del cliente.
     */
    public Optional<DocumentClient> obtenirDni(String dni) {

        try {
            Optional<ClientJpaEntity> resultat = findByDNI(dni);

            if (resultat.isEmpty()) {
                return Optional.empty();
            }

            ClientJpaEntity client = resultat.get();

            if (client.getFotoDniAnverso() == null) {
                return Optional.empty();
            }

            return Optional.of(
                    new DocumentClient(
                            client.getFotoDniAnverso(),
                            client.getFotoDniAnversoTipus()));

        } catch (RuntimeException e) {
            registrarError("obtenirDni", e);
            throw e;
        }
    }

    /**
     * Obtiene la imagen del permiso de conducir del cliente.
     */
    public Optional<DocumentClient> obtenirLlicencia(String dni) {

        try {
            Optional<ClientJpaEntity> resultat = findByDNI(dni);

            if (resultat.isEmpty()) {
                return Optional.empty();
            }

            ClientJpaEntity client = resultat.get();

            if (client.getFotoPermiso() == null) {
                return Optional.empty();
            }

            return Optional.of(
                    new DocumentClient(
                            client.getFotoPermiso(),
                            client.getFotoPermisoTipus()));

        } catch (RuntimeException e) {
            registrarError("obtenirLlicencia", e);
            throw e;
        }
    }

    /**
     * Guarda los cambios de un cliente en la base de datos.
     */
    @Transactional
    public ClientJpaEntity guardarClient(ClientJpaEntity client) {

        try {
            ClientJpaEntity clientGuardat = clientRepository.save(client);

            logQueryService.registrarInfo(
                    "CLIENTS",
                    "Cliente actualizado correctamente. ID: "
                            + clientGuardat.getId());

            return clientGuardat;

        } catch (RuntimeException e) {
            registrarError("guardarClient", e);
            throw e;
        }
    }

    /**
     * Actualiza los datos personales de un cliente.
     */
    @Transactional
    public ClientJpaEntity actualizarClientAdmin(
            String dni,
            ClientJpaEntity datos,
            MultipartFile fotoDni,
            MultipartFile fotoPermiso) throws IOException {

        ClientJpaEntity client = findByDNI(dni)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se ha encontrado el cliente."));

        try {
            client.setNom(datos.getNom());
            client.setCognoms(datos.getCognoms());
            client.setTelefon(datos.getTelefon());
            client.setEmail(datos.getEmail());
            client.setAdreca(datos.getAdreca());
            client.setNacionalitat(datos.getNacionalitat());
            client.setProvincia(datos.getProvincia());

            // El controlador personal debe enviar null en ambas fotografías.
            if (fotoDni != null && !fotoDni.isEmpty()) {
                validarImatge(fotoDni);
                client.setFotoDniAnverso(fotoDni.getBytes());
                client.setFotoDniAnversoTipus(fotoDni.getContentType());
            }

            if (fotoPermiso != null && !fotoPermiso.isEmpty()) {
                validarImatge(fotoPermiso);
                client.setFotoPermiso(fotoPermiso.getBytes());
                client.setFotoPermisoTipus(fotoPermiso.getContentType());
            }

            return guardarClient(client);

        } catch (IOException | RuntimeException e) {
            registrarError("actualizarClientAdmin", e);
            throw e;
        }
    }

    /**
     * Actualiza exclusivamente la caducidad y la fotografía del DNI.
     */
    @Transactional
    public ClientJpaEntity actualizarDocumentacio(
            String dni,
            DocumentacioForm documentacioForm,
            MultipartFile fotoDni) throws IOException {

        try {
            ClientJpaEntity client = findByDNI(dni)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se ha encontrado el cliente."));

            // Comprobar que el DNI recibido coincide con el de la URL.
            if (documentacioForm.getNumeroDocument() != null
                    && !dni.equalsIgnoreCase(
                            documentacioForm.getNumeroDocument().trim())) {

                throw new IllegalArgumentException(
                        "No está permitido modificar el DNI.");
            }

            LocalDate caducitatDni = documentacioForm.getDataCaducitat();

            if (caducitatDni == null) {
                throw new IllegalArgumentException(
                        "La fecha de caducidad del DNI es obligatoria.");
            }

            if (!caducitatDni.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "La fecha de caducidad del DNI debe ser futura.");
            }

            // Actualizar únicamente la caducidad del DNI.
            client.setCadDNI(caducitatDni.toString());

            if (fotoDni != null && !fotoDni.isEmpty()) {
                validarImatge(fotoDni);
                client.setFotoDniAnverso(fotoDni.getBytes());
                client.setFotoDniAnversoTipus(fotoDni.getContentType());
            }

            return guardarClient(client);

        } catch (IOException | RuntimeException e) {
            registrarError("actualizarDocumentacio", e);
            throw e;
        }
    }

    /**
     * Actualiza exclusivamente el tipo, la caducidad y la fotografía
     * de la licencia de conducir.
     */
    @Transactional
    public ClientJpaEntity actualizarLlicencia(
            String dni,
            DocumentacioForm documentacioForm,
            MultipartFile fotoPermiso) throws IOException {

        try {
            ClientJpaEntity client = findByDNI(dni)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se ha encontrado el cliente."));

            String tipoLicencia = documentacioForm.getTipusLlicencia();

            if (tipoLicencia == null || tipoLicencia.isBlank()) {
                throw new IllegalArgumentException(
                        "El tipo de licencia es obligatorio.");
            }

            tipoLicencia = tipoLicencia.trim().toUpperCase();

            if (!tipoLicencia.matches(
                    "^(AM|A1|A2|A|B|BE|C1|C1E|C|CE|D1|D1E|D|DE)$")) {

                throw new IllegalArgumentException(
                        "El tipo de licencia seleccionado no es válido.");
            }

            LocalDate caducitatLlicencia =
                    documentacioForm.getDataCaducitatLlicencia();

            if (caducitatLlicencia == null) {
                throw new IllegalArgumentException(
                        "La fecha de caducidad de la licencia es obligatoria.");
            }

            if (!caducitatLlicencia.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "La fecha de caducidad de la licencia debe ser futura.");
            }

            // Actualizar únicamente los datos de la licencia.
            client.setLliconduir(tipoLicencia);
            client.setCdLLiconduir(caducitatLlicencia.toString());

            if (fotoPermiso != null && !fotoPermiso.isEmpty()) {
                validarImatge(fotoPermiso);
                client.setFotoPermiso(fotoPermiso.getBytes());
                client.setFotoPermisoTipus(fotoPermiso.getContentType());
            }

            return guardarClient(client);

        } catch (IOException | RuntimeException e) {
            registrarError("actualizarLlicencia", e);
            throw e;
        }
    }

    /**
     * Actualiza los datos de la tarjeta de un cliente.
     * El número se almacena sin espacios.
     * Si no se introduce un número nuevo, se conserva el anterior.
     */
    @Transactional
    public ClientJpaEntity actualizarTargeta(
            String dni,
            String titular,
            String numero,
            String caducitat) {

        try {
            // IMPORTANTE: obtener la entidad del Optional.
            ClientJpaEntity client = findByDNI(dni)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Client no trobat."));

            // Limpiar el número de tarjeta.
            String numeroLimpio = numero == null
                    ? ""
                    : numero.replaceAll("\\s+", "")
                            .replaceAll("[^0-9]", "");

            // Si se introduce un número nuevo, validarlo.
            // Si está vacío, conservar el número existente.
            if (!numeroLimpio.isBlank()) {

                if (!luhn(numeroLimpio)) {
                    throw new IllegalArgumentException(
                            "El número de targeta no és vàlid.");
                }

                client.setNumtarjetacredit(numeroLimpio);
            }

            // Validar formato MM/yy.
            if (caducitat == null
                    || !caducitat.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {

                throw new IllegalArgumentException(
                        "La caducitat ha de tenir el format MM/yy.");
            }

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("MM/yy");

            YearMonth dataCaducitat;

            try {
                dataCaducitat = YearMonth.parse(caducitat, formatter);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "La data de caducitat no és vàlida.");
            }

            if (dataCaducitat.isBefore(YearMonth.now())) {
                throw new IllegalArgumentException(
                        "La targeta està caducada.");
            }

            // Actualizar titular y caducidad.
            client.setTitularTargeta(
                    titular == null ? null : titular.trim());

            client.setCaducitatTargeta(caducitat.trim());

            return guardarClient(client);

        } catch (RuntimeException e) {
            registrarError(
                    "Error actualitzant targeta del client " + dni, e);
            throw e;
        }
    }

    /**
     * Cambia la contraseña de un cliente.
     */
    @Transactional
    public void canviarContrasenya(
            String dni,
            String nova,
            String confirmacio) {

        try {
            if (nova == null
                    || !nova.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")) {

                throw new IllegalArgumentException(
                        "La contrasenya ha de tenir entre 8 i 72 caràcters, amb lletres i números.");
            }

            if (!nova.equals(confirmacio)) {
                throw new IllegalArgumentException(
                        "Las contraseñas no coinciden.");
            }

            ClientJpaEntity client = findByDNI(dni)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se ha encontrado el cliente."));

            client.setPassword(passwordEncoder.encode(nova));
            guardarClient(client);

        } catch (RuntimeException e) {
            registrarError("canviarContrasenya", e);
            throw e;
        }
    }

    /**
     * Algoritmo de Luhn para validar números de tarjeta.
     */
    private static boolean luhn(String n) {

        if (n == null || !n.matches("\\d{13,19}")) {
            return false;
        }

        int sum = 0;
        boolean alt = false;

        for (int i = n.length() - 1; i >= 0; i--) {
            int d = n.charAt(i) - '0';

            if (alt) {
                d *= 2;

                if (d > 9) {
                    d -= 9;
                }
            }

            sum += d;
            alt = !alt;
        }

        return sum % 10 == 0;
    }

    /**
     * Comprueba que el archivo sea una imagen y no supere los 5 MB.
     */
    private void validarImatge(MultipartFile fitxer) {

        if (fitxer.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "La imagen no puede superar los 5 MB.");
        }

        String tipus = fitxer.getContentType();

        if (tipus == null || !tipus.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "El archivo seleccionado debe ser una imagen.");
        }
    }

    /**
     * Registra un error en el sistema de logs.
     */
    private void registrarError(String operacio, Exception e) {

        logQueryService.registrarError(
                "CLIENTS",
                "Error en " + operacio + ": " + e.getMessage());
    }
}


package cat.copernic.nothinganpri.controller;

import cat.copernic.nothinganpri.dto.AgentForm;
import cat.copernic.nothinganpri.dto.CanviSolicitud;
import cat.copernic.nothinganpri.entity.AgentJpaEntity;
import cat.copernic.nothinganpri.entity.ClientJpaEntity;
import cat.copernic.nothinganpri.entity.SolicitudAnulacioJpaEntity;
import cat.copernic.nothinganpri.entity.SolicitudValidacionJpaEntity;
import cat.copernic.nothinganpri.service.AgentService;
import cat.copernic.nothinganpri.service.ClientService;
import cat.copernic.nothinganpri.service.LogQueryService;
import cat.copernic.nothinganpri.service.SolicitudAnulacioService;
import cat.copernic.nothinganpri.service.SolicitudValidacionService;
import cat.copernic.nothinganpri.validacio.ProvinciaValidator;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
public class AgentController {

    private final AgentService agentService;
    private final ClientService clientService;
    private final SolicitudValidacionService solicitudService;
    private final SolicitudAnulacioService solicitudAnulacioService;
    private final LogQueryService logQueryService;

    public AgentController(
            AgentService agentService,
            ClientService clientService,
            SolicitudValidacionService solicitudService,
            SolicitudAnulacioService solicitudAnulacioService,
            LogQueryService logQueryService) {

        this.agentService = agentService;
        this.clientService = clientService;
        this.solicitudService = solicitudService;
        this.solicitudAnulacioService = solicitudAnulacioService;
        this.logQueryService = logQueryService;
    }

    /**
     * Carga les províncies disponibles al model.
     */
    private void carregarProvincies(Model model) {
        model.addAttribute(
                "provincias",
                ProvinciaValidator.obtenirProvincies()
                        .stream()
                        .sorted()
                        .toList()
        );
    }

    /**
     * Mostra tots els clients.
     */
    @GetMapping("/agent/clients")
    public String listarClients(Model model) {

        List<ClientJpaEntity> clients = clientService.cercarFiltres(
                null, null, null, null, null, null
        );

        model.addAttribute(
                "clients",
                clients != null ? clients : Collections.emptyList()
        );

        carregarProvincies(model);

        model.addAttribute("nacionalitat", null);
        model.addAttribute("provincia", null);
        model.addAttribute("dni", null);
        model.addAttribute("cognoms", null);
        model.addAttribute("telefon", null);
        model.addAttribute("email", null);

        carregarSolicitudes(model);

        return "llistat-clients";
    }

    /**
     * RF26: Filtra clients per nacionalitat, província, DNI,
     * cognoms, telèfon i email. Els filtres es poden combinar.
     */
    @PostMapping("/agent/clients")
    public String cercarClients(
            @RequestParam(required = false) String nacionalitat,
            @RequestParam(required = false) String provincia,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String cognoms,
            @RequestParam(required = false) String telefon,
            @RequestParam(required = false) String email,
            Model model) {

        List<ClientJpaEntity> clients = clientService.cercarFiltres(
                nacionalitat,
                provincia,
                dni,
                cognoms,
                telefon,
                email
        );

        model.addAttribute(
                "clients",
                clients != null ? clients : Collections.emptyList()
        );

        carregarProvincies(model);

        model.addAttribute("nacionalitat", nacionalitat);
        model.addAttribute("provincia", provincia);
        model.addAttribute("dni", dni);
        model.addAttribute("cognoms", cognoms);
        model.addAttribute("telefon", telefon);
        model.addAttribute("email", email);

        carregarSolicitudes(model);

        return "llistat-clients";
    }

    /**
     * Carga les sol·licituds pendents i les classifica per tipus.
     */
    private void carregarSolicitudes(Model model) {

        List<SolicitudValidacionJpaEntity> pendents =
                solicitudService.obtenerPendientes();

        if (pendents == null) {
            pendents = Collections.emptyList();
        }

        List<SolicitudValidacionJpaEntity> registresInicials =
                pendents.stream()
                        .filter(s -> esTipus(s, "REGISTRO"))
                        .collect(Collectors.toList());

        List<SolicitudValidacionJpaEntity> solicitudesDatos =
                pendents.stream()
                        .filter(s -> esTipus(s, "DATOS_PERSONALES"))
                        .collect(Collectors.toList());

        List<SolicitudValidacionJpaEntity> solicitudesDocumentacion =
                pendents.stream()
                        .filter(s -> esTipus(s, "DOCUMENTACION"))
                        .collect(Collectors.toList());

        List<SolicitudValidacionJpaEntity> solicitudesTarjeta =
                pendents.stream()
                        .filter(s -> esTipus(s, "TARJETA"))
                        .collect(Collectors.toList());

        model.addAttribute("solicitudPorCliente", registresInicials);
        model.addAttribute("solicitudesDatos", solicitudesDatos);
        model.addAttribute("solicitudesDocumentacion", solicitudesDocumentacion);
        model.addAttribute("solicitudesTarjeta", solicitudesTarjeta);

        List<SolicitudAnulacioJpaEntity> solicitudesAnulacion =
                solicitudAnulacioService.obtenerPendientes();

        model.addAttribute(
                "solicitudAnulacions",
                solicitudesAnulacion != null
                        ? solicitudesAnulacion
                        : Collections.emptyList()
        );

        Map<Long, List<CanviSolicitud>> canvisPerSolicitud =
                new HashMap<>();

        for (SolicitudValidacionJpaEntity solicitud : pendents) {

            if (solicitud == null || solicitud.getId() == null) {
                continue;
            }

            canvisPerSolicitud.put(
                    solicitud.getId(),
                    solicitudService.obtenerComparativa(solicitud)
            );
        }

        model.addAttribute("canvisPerSolicitud", canvisPerSolicitud);
    }

    private boolean esTipus(
            SolicitudValidacionJpaEntity solicitud,
            String tipus) {

        return solicitud != null
                && solicitud.getTipo() != null
                && tipus.equalsIgnoreCase(solicitud.getTipo());
    }

    @GetMapping("/agent/solicitudes")
    public String mostrarSolicitudes(Model model) {

        List<SolicitudValidacionJpaEntity> solicitudes =
                solicitudService.obtenerPendientes();

        model.addAttribute(
                "solicitudes",
                solicitudes != null
                        ? solicitudes
                        : Collections.emptyList()
        );

        return "agent/solicitudes";
    }

    @PostMapping("/agent/solicitudes/aprobar")
    public String aprobarRegistro(
            @RequestParam("id") Long solicitudId,
            Principal principal,
            RedirectAttributes redirect) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            solicitudService.aprobarRegistro(
                    solicitudId,
                    principal.getName()
            );

            redirect.addFlashAttribute(
                    "missatge",
                    "El registre s'ha aprovat correctament."
            );

        } catch (Exception e) {
            registrarErrorControlador("aprovar el registre", e);

            redirect.addFlashAttribute(
                    "error",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "No s'ha pogut aprovar el registre."
            );
        }

        return "redirect:/agent/clients";
    }

    @PostMapping("/agent/solicitudes/aprobar-cambios")
    public String aprobarCambios(
            @RequestParam("id") Long solicitudId,
            Principal principal,
            RedirectAttributes redirect) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            solicitudService.aprobarSolicitud(
                    solicitudId,
                    principal.getName()
            );

            redirect.addFlashAttribute(
                    "missatge",
                    "Els canvis s'han aprovat correctament."
            );

        } catch (Exception e) {
            registrarErrorControlador("aprovar els canvis", e);

            redirect.addFlashAttribute(
                    "error",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "No s'han pogut aprovar els canvis."
            );
        }

        return "redirect:/agent/clients";
    }

    @GetMapping("/agent/solicitudes/rebutjar")
    public String mostrarFormulariRebuig(
            @RequestParam("id") Long solicitudId,
            Model model,
            RedirectAttributes redirect) {

        List<SolicitudValidacionJpaEntity> pendents =
                solicitudService.obtenerPendientes();

        if (pendents == null) {
            pendents = Collections.emptyList();
        }

        SolicitudValidacionJpaEntity solicitud =
                pendents.stream()
                        .filter(s -> s != null && s.getId() != null)
                        .filter(s -> solicitudId.equals(s.getId()))
                        .findFirst()
                        .orElse(null);

        if (solicitud == null) {
            logQueryService.registrarWarning(
                    "AGENT",
                    "Intent de consultar un formulari de rebuig d'una sol·licitud inexistent o ja revisada."
            );

            redirect.addFlashAttribute(
                    "error",
                    "La sol·licitud no existeix o ja ha estat revisada."
            );

            return "redirect:/agent/clients";
        }

        model.addAttribute("solicitud", solicitud);

        Map<String, String> datosLegibles = Collections.emptyMap();

        String json = solicitud.getDatosPropuestos();

        if (json != null && !json.isBlank()) {
            Map<String, String> resultado =
                    solicitudService.obtenerDatosLegibles(json);

            if (resultado != null) {
                datosLegibles = resultado;
            }
        }

        model.addAttribute("datosLegibles", datosLegibles);
        model.addAttribute("fotoDniProposada", null);
        model.addAttribute("fotoPermisProposat", null);

        if (esTipus(solicitud, "DOCUMENTACION")) {

            model.addAttribute(
                    "fotoDniProposada",
                    solicitudService.obtenerFotoDni(json)
            );

            model.addAttribute(
                    "fotoPermisProposat",
                    solicitudService.obtenerFotoPermiso(json)
            );
        }

        return "rebutjar-registre";
    }

    @PostMapping("/agent/solicitudes/rechazar")
    public String rechazarSolicitud(
            @RequestParam("id") Long solicitudId,
            @RequestParam("motivo") String motivo,
            Principal principal,
            RedirectAttributes redirect) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            solicitudService.rechazarSolicitud(
                    solicitudId,
                    principal.getName(),
                    motivo
            );

            redirect.addFlashAttribute(
                    "missatge",
                    "La sol·licitud s'ha rebutjat correctament."
            );

        } catch (Exception e) {
            registrarErrorControlador("rebutjar la sol·licitud", e);

            redirect.addFlashAttribute(
                    "error",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "No s'ha pogut rebutjar la sol·licitud."
            );
        }

        return "redirect:/agent/clients";
    }

    @GetMapping("/admin/agents")
    public String llistarAgents(Model model) {

        List<AgentJpaEntity> agents =
                agentService.cercarFiltres(null, true);

        model.addAttribute(
                "agents",
                agents != null ? agents : Collections.emptyList()
        );

        model.addAttribute("estat", "true");

        return "gestio-agents";
    }

    @GetMapping("/admin/agents/nou")
    public String mostrarFormulariAgent(Model model) {

        AgentJpaEntity agent = new AgentJpaEntity();
        agent.setDataContracte(LocalDate.now());

        model.addAttribute("agent", agent);

        return "nou-agent";
    }

    @PostMapping("/admin/agents/guardar")
    public String guardarAgent(
            @ModelAttribute("agent") AgentJpaEntity agent,
            Model model) {

        try {
            agentService.guardar(agent);
            return "redirect:/admin/agents";

        } catch (IllegalArgumentException e) {
            registrarErrorControlador("guardar l'agent", e);

            model.addAttribute("error", e.getMessage());
            return "nou-agent";

        } catch (Exception e) {
            registrarErrorControlador("guardar l'agent", e);

            model.addAttribute(
                    "error",
                    "No s'ha pogut guardar l'agent."
            );
            return "nou-agent";
        }
    }

    @PostMapping("/admin/agents/filtrar")
    public String filtrarAgents(
            @RequestParam(value = "dni", required = false) String dni,
            @RequestParam(value = "activo", required = false) Boolean activo,
            Model model) {

        List<AgentJpaEntity> agents =
                agentService.cercarFiltres(dni, activo);

        model.addAttribute(
                "agents",
                agents != null ? agents : Collections.emptyList()
        );

        model.addAttribute("dni", dni);
        model.addAttribute(
                "estat",
                activo == null ? "" : activo.toString()
        );

        return "gestio-agents";
    }

    @GetMapping("/admin/agents/dni/{dni}")
    public String veureFitxaAgent(
            @PathVariable String dni,
            Model model,
            RedirectAttributes redirect) {

        AgentJpaEntity agent = agentService.buscarAgentPerDNI(dni);

        if (agent == null) {
            logQueryService.registrarWarning(
                    "AGENT",
                    "Intent de consultar la fitxa d'un agent inexistent."
            );

            redirect.addFlashAttribute(
                    "error",
                    "No s'ha trobat l'agent."
            );

            return "redirect:/admin/agents";
        }

        model.addAttribute("agent", agent);

        return "informacio-agents";
    }

    @GetMapping("/admin/agents/editar/{id}")
    public String mostrarEditarAgent(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirect) {

        AgentJpaEntity agent = agentService.buscarPerId(id);

        if (agent == null) {
            logQueryService.registrarWarning(
                    "AGENT",
                    "Intent de modificar un agent inexistent."
            );

            redirect.addFlashAttribute(
                    "error",
                    "No s'ha trobat l'agent."
            );

            return "redirect:/admin/agents";
        }

        model.addAttribute("agent", agent);

        return "editar-agent";
    }

    /**
     * Actualitza un agent. El formulari es valida amb AgentForm.
     */
    @PostMapping("/admin/agents/editar/{id}")
    public String actualizarAgent(
            @PathVariable Long id,
            @Valid @ModelAttribute("agent") AgentForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirect) {

        form.setId(id);

        if (bindingResult.hasErrors()) {
            return "editar-agent";
        }

        try {
            agentService.actualizar(id, form);

            redirect.addFlashAttribute(
                    "missatge",
                    "L'agent s'ha actualitzat correctament."
            );

            return "redirect:/admin/agents";

        } catch (IllegalArgumentException e) {
            registrarErrorControlador("actualitzar l'agent", e);

            model.addAttribute("error", e.getMessage());

            return "editar-agent";

        } catch (Exception e) {
            registrarErrorControlador("actualitzar l'agent", e);

            model.addAttribute(
                    "error",
                    "No s'ha pogut actualitzar l'agent."
            );

            return "editar-agent";
        }
    }

    @PostMapping("/admin/agents/desactivar/{id}")
    public String desactivarAgent(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        try {
            agentService.desactivar(id);

            redirect.addFlashAttribute(
                    "missatge",
                    "L'agent s'ha desactivat correctament."
            );

        } catch (IllegalArgumentException e) {
            registrarErrorControlador("desactivar l'agent", e);
            redirect.addFlashAttribute("error", e.getMessage());

        } catch (Exception e) {
            registrarErrorControlador("desactivar l'agent", e);

            redirect.addFlashAttribute(
                    "error",
                    "No s'ha pogut desactivar l'agent."
            );
        }

        return "redirect:/admin/agents";
    }

    @PostMapping("/admin/agents/activar/{id}")
    public String activarAgent(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        try {
            agentService.activar(id);

            redirect.addFlashAttribute(
                    "missatge",
                    "L'agent s'ha activat correctament."
            );

        } catch (IllegalArgumentException e) {
            registrarErrorControlador("activar l'agent", e);
            redirect.addFlashAttribute("error", e.getMessage());

        } catch (Exception e) {
            registrarErrorControlador("activar l'agent", e);

            redirect.addFlashAttribute(
                    "error",
                    "No s'ha pogut activar l'agent."
            );
        }

        return "redirect:/admin/agents";
    }

    private void registrarErrorControlador(
            String operacion,
            Exception e) {

        if (e instanceof IllegalArgumentException
                || e instanceof IllegalStateException) {

            logQueryService.registrarWarning(
                    "AGENT",
                    "No s'ha pogut " + operacion + ": " + e.getMessage()
            );

        } else {
            logQueryService.registrarError(
                    "AGENT",
                    "Error inesperat en intentar " + operacion + ": " + e.getMessage()
            );
        }
    }
}

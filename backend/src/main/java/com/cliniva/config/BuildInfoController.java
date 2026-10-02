package com.cliniva.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Metadados do build, para responder "que versão está no ar?".
 *
 * <p>Existe por causa de um incidente: o deploy do Render falhou porque a
 * aplicação não sobe com schema divergente, o processo morre antes de abrir
 * a porta e o Render <b>continua servindo o container antigo</b>. O
 * {@code /actuator/health} continua respondendo 200 e não dá para saber que
 * o deploy quebrou.
 *
 * <p>Comparando o {@code commit} desta rota com o da branch {@code main},
 * dá para detectar deploy falho só com um curl.
 *
 * <p>Só expõe metadados de build — nenhum segredo.
 */
@RestController
public class BuildInfoController {

    private final String commit;

    public BuildInfoController(
            @Value("${cliniva.build.commit:desconhecido}") String commit) {
        this.commit = commit;
    }

    @GetMapping("/api/saude/build")
    public Map<String, String> build() {
        return Map.of(
                "status", "ok",
                "commit", commit,
                "versao", "0.4.0");
    }
}

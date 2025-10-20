package com.desafiotecnico.desafiomagalu.controller;

import com.desafiotecnico.desafiomagalu.dto.UserResponseDto;
import com.desafiotecnico.desafiomagalu.exception.BadFileFormatException;
import com.desafiotecnico.desafiomagalu.service.FileProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Operações relacionadas ao upload e consulta de pedidos")
public class OrderController {

    private final FileProcessingService fileProcessingService;

    @Operation(
            summary = "Faz upload de arquivo de pedidos",
            description = "Recebe um arquivo .txt contendo informações de pedidos e retorna os dados processados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo processado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou formato incorreto"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar o arquivo")
    })
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<UserResponseDto>> uploadFile(
            @Parameter(description = "Arquivo .txt a ser enviado", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<UserResponseDto> result = fileProcessingService.processFile(file);
            return ResponseEntity.ok(result);
        } catch (BadFileFormatException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Consulta pedidos",
            description = "Permite consultar pedidos por ID e/ou intervalo de datas (yyyy-MM-dd)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta realizada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserResponseDto>> getOrders(
            @Parameter(description = "ID do pedido", example = "753")
            @RequestParam(name = "orderId", required = false) Long orderId,

            @Parameter(description = "Data inicial (yyyy-MM-dd)", example = "2021-01-01")
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Data final (yyyy-MM-dd)", example = "2021-12-31")
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            Optional<Long> optOrderId = Optional.ofNullable(orderId);
            Optional<LocalDate> optStart = Optional.ofNullable(startDate);
            Optional<LocalDate> optEnd = Optional.ofNullable(endDate);

            List<UserResponseDto> result = fileProcessingService.query(optOrderId, optStart, optEnd);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
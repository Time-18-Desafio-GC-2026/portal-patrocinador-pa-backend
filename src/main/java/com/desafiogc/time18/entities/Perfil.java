package com.desafiogc.time18.entities;

public enum Perfil {
    SUPERADMIN,   // Conta mestre da RBS criada via variável de ambiente
    ADMIN,        // Time B2B da RBS (alimenta o portal, cria marcas e sobe relatórios)
    PATROCINADOR  // Cliente ou agência (vê apenas os dados da própria marca)
}
# ChurnGuard

[![CI](https://github.com/ELKHALIFA-AYMEN/churnguard/actions/workflows/ci.yml/badge.svg)](https://github.com/ELKHALIFA-AYMEN/churnguard/actions/workflows/ci.yml)

Plateforme SaaS de prédiction du churn clients : import de données, scoring par machine learning, explications et recommandations d'action.

## Stack

- **Backend** : Java 21, Spring Boot, Spring Security (JWT), PostgreSQL, Flyway
- **ML** : Python, FastAPI, scikit-learn, SHAP *(à venir)*
- **Frontend** : React, TypeScript *(à venir)*
- **Infra et qualité** : Docker Compose, Testcontainers, GitHub Actions

## Fonctionnalités actuelles

- Inscription d'une organisation et de son premier administrateur
- Connexion avec jeton JWT (HS256, expiration courte)
- Rôles `ADMIN`, `ANALYST`, `VIEWER` et contrôle d'accès par rôle
- Isolation des données entre organisations (multi-tenant)
- Mots de passe hachés avec BCrypt, validation des entrées, erreurs normalisées
- 17 tests d'intégration sur une base PostgreSQL jetable

## Lancer le projet

Prérequis : Java 21, Docker.

1. Copier `.env.example` en `.env` et renseigner les valeurs (mot de passe de la base, secret JWT).
2. Démarrer la base : `docker compose up -d`
3. Lancer le backend : `cd backend` puis `./mvnw spring-boot:run` (Windows : `mvnw.cmd spring-boot:run`)

## Lancer les tests

```
cd backend
./mvnw verify
```

Docker doit être démarré : les tests utilisent Testcontainers.

## Statut

En cours de développement.
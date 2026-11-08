# ChurnGuard : interface web

Application React + TypeScript construite avec Vite. Elle consomme l'API du back-end (`/api/...`).

## Commandes

À lancer dans ce dossier (dans le Dev Container) :

| Commande | Rôle |
|---|---|
| `npm ci` | Installe les dépendances exactement comme dans `package-lock.json` |
| `npm run dev` | Serveur de développement sur http://localhost:5173 |
| `npm run test` | Tests (Vitest et Testing Library) |
| `npm run lint` | Vérification ESLint |
| `npm run build` | Vérification des types et construction pour la production |

En développement, le proxy de `vite.config.ts` redirige `/api` vers le back-end (`http://host.docker.internal:8080`) : le navigateur ne parle qu'à une seule origine, sans configuration CORS ouverte.

## Organisation de `src/`

| Dossier | Contenu |
|---|---|
| `api/` | Client HTTP (`client.ts`) et types partagés avec l'API |
| `auth/` | Session, contexte d'authentification, protection des routes, rôles |
| `layout/` | Barre de navigation et mise en page commune |
| `pages/` | Connexion, tableau de bord, fiche client, import des données |
| `test/` | Configuration des tests et données de test |

## Choix de sécurité côté navigateur

- Le client d'API n'accepte que des chemins `/api/...` : le jeton ne peut pas être envoyé vers un autre site.
- Le jeton est conservé dans `sessionStorage` (effacé à la fermeture de l'onglet). Un cookie `HttpOnly` serait plus robuste face aux failles XSS ; le compromis est assumé.
- Une expiration ou une réponse 401 déconnecte automatiquement l'utilisateur.
- L'interface masque les actions interdites au rôle de l'utilisateur, par confort. **La vraie protection est côté serveur**, et elle est testée.
- Les messages d'erreur affichés sont écrits en dur : aucun texte venant du serveur n'est injecté tel quel dans la page.

Le contexte complet du projet est dans le [README principal](../README.md).
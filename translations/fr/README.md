# bi - Plateforme BI Intelligente

> Auteur : [lyj](https://github.com/)



## Introduction au Projet

### Présentation du Projet

Une **plateforme BI intelligente** basée sur React + Spring Boot + MQ + AIGC.

Projets traditionnels (plateformes d'analyse de données) : L'analyse est effectuée par des analystes de données professionnels.
Mon projet : Les utilisateurs n'ont qu'à saisir l'objectif d'analyse et télécharger les données brutes, et le système utilisera l'IA pour générer automatiquement des graphiques visuels et des conclusions d'analyse.


### Processus Métier
① Le client saisit la demande d'analyse et les données brutes, puis envoie une requête au backend métier. ② Le backend métier utilise le service IA pour traiter les données du client, les stocker dans la base de données, et générer des graphiques. ③ Les données traitées sont envoyées par le backend métier au service IA, qui génère les résultats et les renvoie au backend, qui les retourne finalement au client pour affichage.
<img width="987" height="648" alt="image" src="https://github.com/user-attachments/assets/8285eca1-fb46-43e2-b8b1-96155d5dcea6" />





### Fonctionnalités Métier

- Connexion, inscription, déconnexion, mise à jour, recherche, gestion des permissions
- Analyse IA de graphiques, affichage visuel



## Choix Technologiques

### Backend

- Framework de développement Java Spring Boot
- Couche de stockage : Base de données MySQL + Cache Redis
- Génération automatique avec MyBatis-Plus et MyBatis X
- Verrouillage distribué avec Redisson
- Cache local avec Caffeine
- ⭐️ Capacités IA génériques basées sur le modèle deepseek
- ⭐️ Outil de limitation de débit intégré à redisson
- ⭐️ Transformation asynchrone locale avec pool de threads
- ⭐️ Transformation asynchrone distribuée avec MQ
- ⭐️ Sécurité des fichiers téléchargés garantie


### Outils
- Traitement de tableaux avec Easy Excel
- Bibliothèque d'outils Hutool
- Classes utilitaires Apache Commons Lang3
- Annotations Lombok
- IDE Frontend : VsCode
- IDE Backend : JetBrains IDEA
- [Assistant de programmation intelligent CodeGeeX](https://codegeex.cn/)
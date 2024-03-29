export const editReportFR = {
    sections: {
        selectTournament: "Sélectionner un tournoi",
        selectTeams: "Sélectionner des équipes",
        editGameReports: "Modifier les rapports de match",
        editPitchReports: "Modifier les rapports de terrain",
        submitReport: "Soumettre",
    },
    report: {
        start: 'Commencer le rapport',
        submit: 'Soumettre le rapport',
        issues: {
            hasFollowingIssues: 'présente les problèmes suivants',
            noTournamentSelected: 'Aucun tournoi n’est actuellement sélectionné pour ce rapport. Veuillez corriger cela avant de soumettre.',
            noGameTypeSelected: 'Aucun type de jeu sélectionné',
            noStartingTimeSelected: 'Aucune heure de début sélectionnée',
            noExtraTimeOptionSelected: 'Aucune option de temps supplémentaire sélectionnée',
            noTeamASelcted: 'Aucune équipe A sélectionnée',
            noTeamBSelcted: 'Aucune équipe B sélectionnée',
            teamAEqualTeamB: 'L’équipe A et l’équipe B sont les mêmes',
            noScoresEntered: 'Aucun score saisi - Cela pourrait être correct si le match était un match nul.',
            goToGameReport: 'Aller au rapport de match',
            injuryNoDetails: 'Aucun détail saisi pour blessure de',
            injuryNoName: 'Nom manquant pour blessure avec détails',
            disciplinaryActionNoName: 'Nom manquant pour action disciplinaire avec détails',
            disciplinaryActionNoNumber: 'Numéro manquant pour action disciplinaire de',
            disciplinaryActionNoRule: 'Règle manquante pour action disciplinaire de',
            pitchNoName: 'Aucun nom saisi',
            pitchNoSurface: 'Aucune surface sélectionnée',
            pitchMissingDimension: 'Au moins une dimension du terrain manquante',
            pitchMarkingsIncomplete: 'Au moins un marquage du terrain incomplet',
            pitchGoalInfoIncomplete: 'Informations sur les buts incomplètes',
        },
        updatingDatabase: 'Mise à jour de la base de données...',
        additionalInformation: 'Commentaires supplémentaires pour le rapport',
        couldNotSaveDataConfirm: 'Impossible de sauvegarder les données avant de quitter la page. Êtes-vous sûr de vouloir partir ?',
        couldNotSaveDataTitle: 'Impossible de sauvegarder les données',
        couldNotSaveDataContinue: 'Continuer l’édition',
        couldNotSaveDataLeave: 'Quitter sans sauvegarder',
    },
    navigation: {
        backToDashboard: 'Retour au tableau de bord',
        loading: 'Chargement...',
    },
    tournament: {
        select: 'Sélectionner un tournoi',
        selected: 'Tournoi sélectionné',
        date: 'Date du tournoi',
        recent: 'Affichage des tournois récents',
        notInListCreateQuestion: 'Tournoi non listé ? Créer un nouveau ...',
        create: 'Créer un nouveau tournoi',
        changeDate: 'Changer la date',
        enterName: 'Entrer le nom du tournoi',
        enterLocation: 'Entrer le lieu du tournoi',
        selectRegion: 'Sélectionner une région',
        isLeague: 'Ronde de ligue (s’étend sur plus d’un jour)',
        leagueInfo: 'Les rondes de ligue sont une série de jeux qui sont joués sur plus d’un jour. C’est souvent le cas pour une ligue. Ici, nous collectons tous les jeux d’une ronde de la ligue. Ces événements ont une date de début et une date de fin.',
        dateinput: {
            tournament: 'Date du tournoi',
            startLeague: 'Date de début de la ronde',
            endLeague: 'Date de fin de la ronde',
        },
        error: {
            noEndDate: 'Si le tournoi est une ronde de ligue, vous devez entrer une date de fin',
            missingFields: 'Veuillez remplir tous les champs',
        }
    },
    teamSelect: {
        selectReportTeams: 'Sélectionner les clubs qui feront partie de ce rapport',
        enterNameForSearch: 'Entrer le nom de l’équipe pour recherche',
        addNewTeam: 'Ajouter une nouvelle équipe',
        addNewAmalgamation: 'Ajouter une nouvelle fusion',
        alreadyInSelection: 'Déjà dans la sélection',
        newTeamName: 'Nom de la nouvelle équipe',
        newAmalgamationTitle: 'Créer une nouvelle fusion',
        newAmalgamationName: 'Nom de la nouvelle fusion',
        warningAlreadyExistingAmalgamation: 'AVERTISSEMENT : La fusion avec votre sélection existe déjà',
        selectedTeams: 'Équipes sélectionnées',
        newSquadsButton: 'Créer des sous-équipes',
        useSquadPrefix: 'Utilise',
        removeFromList: 'Retirer de la liste',
        splitTeam: {
            title: 'Nouvelle sous-équipe',
            infoSelected: 'Équipe de base sélectionnée',
            selectNumberOf: 'Sélectionnez le nombre de sous-équipes à créer',
            wouldCreateFollowing: 'Cette action créerait les sous-équipes suivantes',
            modifyNames: 'Modifier les noms',
            nameExists: 'Ce nom existe déjà',
        }
    },
    gameReport: {
        deleteConfirmTitle: 'Supprimer le rapport de match',
        deleteConfirmText: 'Êtes-vous sûr de vouloir supprimer ce rapport de match ?',
        throwInTime: 'Heure du lancer',
        umpiresPresentOnTime: 'Arbitres présents à l’heure',
        commentOnUmpires: 'Commentaire sur les arbitres',
        extraTime: 'Temps supplémentaire',
        gameType: 'Type de jeu',
        deleteGameReport: 'Supprimer ce rapport de match',
        teamA: 'Équipe A',
        teamB: 'Équipe B',
        notes: 'Notes',
        selectTeam: 'Sélectionner une équipe',
        goals: 'Buts',
        points: 'Points',
        total: 'Total',
        editDisciplinaryActions: 'Modifier les actions disciplinaires',
        editInjuries: 'Modifier les blessures',
        disciplinaryActionTitle: 'Actions disciplinaires pour',
        injuriesTitle: 'Blessures pour',
        player: {
            firstName: 'Prénom',
            lastName: 'Nom de famille',
            number: 'Numéro',
        },
        rule: "Règle",
        description: "Description",
        addGameType: 'Ajouter un type de jeu',
        newGameType: 'Nouveau type de jeu',
        gameTypeExistingAlternatives: 'Alternatives existantes',
        alertIfBothTeamsAreTheSame: 'Vous avez sélectionné la même équipe pour les deux équipes. Veuillez corriger cela avant de soumettre le rapport.',
    },
    pitchReport: {
        unnamedPitch: 'Terrain sans nom',
        noPitchReports: 'Aucun rapport de terrain pour le moment. Créez le premier',
        confirmDeleteTitle: 'Supprimer le rapport de terrain',
        confirmDeleteText: 'Êtes-vous sûr de vouloir supprimer ce rapport de terrain ?',
        enterNameReminder: 'Veuillez entrer un nom pour le terrain ! Sinon, les données ne seront pas stockées.',
        pitchNamePlaceholder: 'Terrain #X',
        name: 'Nom',
        surface: 'Surface',
        length: 'Longueur',
        width: 'Largeur',
        smallSquareMarkings: 'Marquages du petit carré',
        penaltySquareMarkings: 'Marquages de la surface de réparation',
        thirteenMeterMarkings: 'Marquages de 13 m',
        twentyMeterMarkings: 'Marquages de 20 m',
        longMarkings: 'Marquages de 45/65 m',
        goalPosts: 'Poteaux de but',
        goalDimensions: 'Dimensions des buts',
        additionalInformation: 'Informations supplémentaires',
        delete: 'Supprimer ce rapport de terrain',
    },
    general: {
        submit: 'Soumettre',
        cancel: 'Annuler',
        delete: 'Supprimer',
    }
};

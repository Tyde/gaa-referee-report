export const editReportES = {
    sections: {
        selectTournament: "Seleccionar torneo",
        selectTeams: "Seleccionar equipos",
        editGameReports: "Editar informes de juego",
        editPitchReports: "Editar informes de campo",
        submitReport: "Enviar",
    },
    report: {
        start: 'Iniciar informe',
        submit: 'Enviar informe',
        issues: {
            hasFollowingIssues: 'tiene los siguientes problemas',
            noTournamentSelected: 'Actualmente no hay un torneo seleccionado para este informe. Por favor, soluciona esto antes de enviarlo.',
            noGameTypeSelected: 'No se ha seleccionado tipo de juego',
            noStartingTimeSelected: 'No se ha seleccionado hora de inicio',
            noExtraTimeOptionSelected: 'No se ha seleccionado opción de tiempo extra',
            noTeamASelcted: 'No se ha seleccionado equipo A',
            noTeamBSelcted: 'No se ha seleccionado equipo B',
            teamAEqualTeamB: 'El equipo A y el equipo B son iguales',
            noScoresEntered: 'No se han ingresado puntuaciones - Esto podría ser correcto si el juego fue un empate.',
            goToGameReport: 'Ir al informe del juego',
            injuryNoDetails: 'No se ingresaron detalles para lesión de',
            injuryNoName: 'Falta nombre para lesión con detalles',
            disciplinaryActionNoName: 'Falta nombre para acción disciplinaria con detalles',
            disciplinaryActionNoNumber: 'Falta número para acción disciplinaria de',
            disciplinaryActionNoRule: 'Falta regla para acción disciplinaria de',
            pitchNoName: 'No se ingresó nombre',
            pitchNoSurface: 'No se seleccionó superficie',
            pitchMissingDimension: 'Falta al menos una dimensión del campo',
            pitchMarkingsIncomplete: 'Falta completar al menos una marca del campo',
            pitchGoalInfoIncomplete: 'Información de la portería incompleta',
        },
        updatingDatabase: 'Actualizando base de datos...',
        additionalInformation: 'Comentarios adicionales para el informe',
        couldNotSaveDataConfirm: 'No se pudieron guardar los datos antes de abandonar la página. ¿Estás seguro de que quieres irte?',
        couldNotSaveDataTitle: 'No se pudieron guardar los datos',
        couldNotSaveDataContinue: 'Continuar editando',
        couldNotSaveDataLeave: 'Salir sin guardar',
    },
    navigation: {
        backToDashboard: 'Volver al tablero',
        loading: 'Cargando...',
    },
    tournament: {
        select: 'Seleccionar un torneo',
        selected: 'Torneo seleccionado',
        date: 'Fecha del torneo',
        recent: 'Mostrando torneos recientes',
        notInListCreateQuestion: '¿Torneo no en la lista? Crear nuevo ...',
        create: 'Crear nuevo torneo',
        changeDate: 'Cambiar fecha',
        enterName: 'Ingresar nombre del torneo',
        enterLocation: 'Ingresar ubicación del torneo',
        selectRegion: 'Seleccionar una región',
        isLeague: 'Ronda de liga (se extiende por más de un día)',
        leagueInfo: 'Las rondas de liga son una serie de juegos que se juegan durante más de un día. Esto suele ser el caso de una liga. Aquí recopilamos todos los juegos de una ronda de la liga. Estos eventos tienen una fecha de inicio y una fecha de finalización.',
        dateinput: {
            tournament: 'Fecha del torneo',
            startLeague: 'Fecha de inicio de la ronda',
            endLeague: 'Fecha de finalización de la ronda',
        },
        error: {
            noEndDate: 'Si el torneo es una ronda de liga, necesitas ingresar una fecha de finalización',
            missingFields: 'Por favor, completa todos los campos',
        }
    },
    teamSelect: {
        selectReportTeams: 'Seleccionar los clubes que serán parte de este informe',
        enterNameForSearch: 'Ingresar nombre del equipo para buscar',
        addNewTeam: 'Agregar nuevo equipo',
        addNewAmalgamation: 'Agregar nueva fusión',
        alreadyInSelection: 'Ya en selección',
        newTeamName: 'Nombre del nuevo equipo',
        newAmalgamationTitle: 'Crear nueva fusión',
        newAmalgamationName: 'Nombre de la nueva fusión',
        warningAlreadyExistingAmalgamation: 'ADVERTENCIA: La fusión con su selección ya existe',
        selectedTeams: 'Equipos seleccionados',
        newSquadsButton: 'Crear subescuadra',
        useSquadPrefix: 'Usar',
        removeFromList: 'Eliminar de la lista',
        splitTeam: {
            title: 'Nueva subescuadra',
            infoSelected: 'Equipo base seleccionado',
            selectNumberOf: 'Seleccionar el número de subescuadras a crear',
            wouldCreateFollowing: 'Esta acción crearía las siguientes subescuadras',
            modifyNames: 'Modificar nombres',
            nameExists: 'Este nombre ya existe'
        }
    },
    gameReport: {
        deleteConfirmTitle: 'Eliminar informe de juego',
        deleteConfirmText: '¿Estás seguro de que quieres eliminar este informe de juego?',
        throwInTime: 'Hora de inicio',
        umpiresPresentOnTime: 'Umpires presentes a tiempo',
        commentOnUmpires: 'Comentar sobre los umpires',
        extraTime: 'Tiempo extra',
        gameType: 'Tipo de juego',
        deleteGameReport: 'Eliminar este informe de juego',
        teamA: 'Equipo A',
        teamB: 'Equipo B',
        notes: 'Notas',
        selectTeam: 'Seleccionar un equipo',
        goals: 'Goles',
        points: 'Puntos',
        total: 'Total',
        editDisciplinaryActions: 'Editar acciones disciplinarias',
        editInjuries: 'Editar lesiones',
        disciplinaryActionTitle: 'Acciones disciplinarias para',
        injuriesTitle: 'Lesiones para',
        player: {
            firstName: 'Nombre',
            lastName: 'Apellido',
            number: 'Número',
        },
        rule: "Regla",
        description: "Descripción",
        addGameType: 'Agregar tipo de juego',
        newGameType: 'Nuevo tipo de juego',
        gameTypeExistingAlternatives: 'Alternativas existentes',
        alertIfBothTeamsAreTheSame: 'Seleccionaste el mismo equipo para ambos equipos. Por favor, corrige esto antes de enviar el informe.',
    },
    pitchReport: {
        unnamedPitch: 'Campo sin nombre',
        noPitchReports: 'Aún no hay informes de campo. Crea el primero',
        confirmDeleteTitle: 'Eliminar informe de campo',
        confirmDeleteText: '¿Estás seguro de que quieres eliminar este informe de campo?',
        enterNameReminder: '¡Por favor ingresa un nombre para el campo! De lo contrario, los datos no se almacenarán.',
        pitchNamePlaceholder: 'Campo #X',
        name: 'Nombre',
        surface: 'Superficie',
        length: 'Longitud',
        width: 'Ancho',
        smallSquareMarkings: 'Marcas de cuadro pequeño',
        penaltySquareMarkings: 'Marcas de área de penalti',
        thirteenMeterMarkings: 'Marcas de 13m',
        twentyMeterMarkings: 'Marcas de 20m',
        longMarkings: 'Marcas de 45m/65m',
        goalPosts: 'Postes de gol',
        goalDimensions: 'Dimensiones del gol',
        additionalInformation: 'Información adicional',
        delete: 'Eliminar este informe de campo',
    },
    general: {
        submit: 'Enviar',
        cancel: 'Cancelar',
        delete: 'Eliminar',
    }
};

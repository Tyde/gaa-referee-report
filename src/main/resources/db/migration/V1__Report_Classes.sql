CREATE TABLE IF NOT EXISTS Users (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name VARCHAR(60) NOT NULL, last_name VARCHAR(60) NOT NULL, password VARBINARY(60) NOT NULL, mail VARCHAR(100) NOT NULL);
CREATE TABLE IF NOT EXISTS Sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid BINARY(16) NOT NULL, "user" BIGINT NOT NULL, expires TEXT NOT NULL, CONSTRAINT fk_Sessions_user__id FOREIGN KEY ("user") REFERENCES Users(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
CREATE TABLE IF NOT EXISTS Teams (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(100) NOT NULL, is_amalgamation BOOLEAN NOT NULL);
CREATE TABLE IF NOT EXISTS Amalgamations (id INTEGER PRIMARY KEY AUTOINCREMENT, amalgamation BIGINT NOT NULL, added_team BIGINT NOT NULL, CONSTRAINT fk_Amalgamations_amalgamation__id FOREIGN KEY (amalgamation) REFERENCES Teams(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Amalgamations_added_team__id FOREIGN KEY (added_team) REFERENCES Teams(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
CREATE TABLE IF NOT EXISTS Tournaments (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(80) NOT NULL, location VARCHAR(90) NOT NULL, "date" DATE NOT NULL);
CREATE TABLE IF NOT EXISTS GameCodes (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(60) NOT NULL);
CREATE TABLE IF NOT EXISTS TournamentReports (id INTEGER PRIMARY KEY AUTOINCREMENT, tournament BIGINT NOT NULL, referee BIGINT NOT NULL, code BIGINT NOT NULL, additional_information TEXT DEFAULT '' NOT NULL, is_submitted BOOLEAN DEFAULT 0 NOT NULL, submit_date TEXT NULL, CONSTRAINT fk_TournamentReports_tournament__id FOREIGN KEY (tournament) REFERENCES Tournaments(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_TournamentReports_referee__id FOREIGN KEY (referee) REFERENCES Users(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_TournamentReports_code__id FOREIGN KEY (code) REFERENCES GameCodes(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
CREATE TABLE IF NOT EXISTS TournamentReportTeamPreSelections (id INTEGER PRIMARY KEY AUTOINCREMENT, report BIGINT NOT NULL, team BIGINT NOT NULL, CONSTRAINT fk_TournamentReportTeamPreSelections_report__id FOREIGN KEY (report) REFERENCES TournamentReports(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_TournamentReportTeamPreSelections_team__id FOREIGN KEY (team) REFERENCES Teams(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
CREATE TABLE IF NOT EXISTS GameTypes (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(60) NOT NULL);
CREATE TABLE IF NOT EXISTS ExtraTimeOptions (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(60) NOT NULL);
CREATE TABLE IF NOT EXISTS GameReports (id INTEGER PRIMARY KEY AUTOINCREMENT, report_id BIGINT NOT NULL, team_a BIGINT NOT NULL, team_b BIGINT NOT NULL, start_time TEXT NOT NULL, game_type BIGINT NULL, team_a_goals INT NOT NULL, team_a_points INT NOT NULL, team_b_goals INT NOT NULL, team_b_points INT NOT NULL, extra_time BIGINT NULL, umpire_present_on_time BOOLEAN DEFAULT 1 NOT NULL, umpire_notes TEXT NULL, CONSTRAINT fk_GameReports_report_id__id FOREIGN KEY (report_id) REFERENCES TournamentReports(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_GameReports_team_a__id FOREIGN KEY (team_a) REFERENCES Teams(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_GameReports_team_b__id FOREIGN KEY (team_b) REFERENCES Teams(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_GameReports_game_type__id FOREIGN KEY (game_type) REFERENCES GameTypes(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_GameReports_extra_time__id FOREIGN KEY (extra_time) REFERENCES ExtraTimeOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
CREATE TABLE IF NOT EXISTS Rules (id INTEGER PRIMARY KEY AUTOINCREMENT, code BIGINT NOT NULL, is_yellow BOOLEAN NOT NULL, is_black BOOLEAN NOT NULL, is_red BOOLEAN NOT NULL, description TEXT NOT NULL, CONSTRAINT fk_Rules_code__id FOREIGN KEY (code) REFERENCES GameCodes(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
CREATE TABLE IF NOT EXISTS DisciplinaryActions (id INTEGER PRIMARY KEY AUTOINCREMENT, game BIGINT NOT NULL, team BIGINT NOT NULL, first_name VARCHAR(80) NOT NULL, last_name VARCHAR(80) NOT NULL, "number" INT NOT NULL, rule BIGINT NOT NULL, details TEXT NOT NULL, CONSTRAINT fk_DisciplinaryActions_game__id FOREIGN KEY (game) REFERENCES GameReports(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_DisciplinaryActions_team__id FOREIGN KEY (team) REFERENCES Teams(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_DisciplinaryActions_rule__id FOREIGN KEY (rule) REFERENCES Rules(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
CREATE TABLE IF NOT EXISTS Injuries (id INTEGER PRIMARY KEY AUTOINCREMENT, game BIGINT NOT NULL, team BIGINT NOT NULL, first_name VARCHAR(80) NOT NULL, last_name VARCHAR(80) NOT NULL, details TEXT NOT NULL, CONSTRAINT fk_Injuries_game__id FOREIGN KEY (game) REFERENCES GameReports(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Injuries_team__id FOREIGN KEY (team) REFERENCES Teams(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
CREATE TABLE IF NOT EXISTS PitchSurfaceOptions (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(80) NOT NULL);
CREATE TABLE IF NOT EXISTS PitchLengthOptions (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(80) NOT NULL);
CREATE TABLE IF NOT EXISTS PitchWidthOptions (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(80) NOT NULL);
CREATE TABLE IF NOT EXISTS PitchMarkingsOptions (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(80) NOT NULL);
CREATE TABLE IF NOT EXISTS PitchGoalpostsOptions (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(80) NOT NULL);
CREATE TABLE IF NOT EXISTS PitchGoalDimensionOptions (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(80) NOT NULL);
CREATE TABLE IF NOT EXISTS Pitches (id INTEGER PRIMARY KEY AUTOINCREMENT, report BIGINT NOT NULL, "name" VARCHAR(80) NOT NULL, surface BIGINT NOT NULL, "length" BIGINT NOT NULL, width BIGINT NOT NULL, small_square_markings BIGINT NOT NULL, penalty_square_markings BIGINT NOT NULL, thirteen_meter_markings BIGINT NOT NULL, twenty_meter_markings BIGINT NOT NULL, long_meter_markings BIGINT NOT NULL, goal_posts BIGINT NOT NULL, goal_dimensions BIGINT NOT NULL, additional_information TEXT NOT NULL, CONSTRAINT fk_Pitches_report__id FOREIGN KEY (report) REFERENCES TournamentReports(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_surface__id FOREIGN KEY (surface) REFERENCES PitchSurfaceOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_length__id FOREIGN KEY ("length") REFERENCES PitchLengthOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_width__id FOREIGN KEY (width) REFERENCES PitchWidthOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_small_square_markings__id FOREIGN KEY (small_square_markings) REFERENCES PitchMarkingsOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_penalty_square_markings__id FOREIGN KEY (penalty_square_markings) REFERENCES PitchMarkingsOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_thirteen_meter_markings__id FOREIGN KEY (thirteen_meter_markings) REFERENCES PitchMarkingsOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_twenty_meter_markings__id FOREIGN KEY (twenty_meter_markings) REFERENCES PitchMarkingsOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_long_meter_markings__id FOREIGN KEY (long_meter_markings) REFERENCES PitchMarkingsOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_goal_posts__id FOREIGN KEY (goal_posts) REFERENCES PitchGoalpostsOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_Pitches_goal_dimensions__id FOREIGN KEY (goal_dimensions) REFERENCES PitchGoalDimensionOptions(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
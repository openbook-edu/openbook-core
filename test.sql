WITH updated AS (
		UPDATE teams_scorers 
			SET leader = 't'
				WHERE team_id = '5528a19e-8bcf-472a-90ff-3d6020ac2322'
					  AND scorer_id = 'cc0b7c1a-5e3c-44a3-8944-407e037a24f0'
						RETURNING scorer_id, team_id, leader, deleted, archived, created_at)
					SELECT users.*, updated.team_id, updated.leader, updated.deleted, updated.archived, updated.created_at AS included_at
					FROM updated, users
					WHERE updated.scorer_id = users.id;

UPDATE guess_list
SET guesses = (
    SELECT jsonb_agg(elem || '{"type": "CLASSIC"}'::jsonb)
    FROM jsonb_array_elements(guesses::jsonb) AS elem
)
WHERE guesses IS NOT NULL
  AND jsonb_typeof(guesses::jsonb) = 'array'
  AND jsonb_array_length(guesses::jsonb) > 0;


UPDATE anonymous_guess_list
SET guesses = jsonb_build_object(
        'CLASSIC',
        (
            SELECT jsonb_agg(elem || '{"type": "CLASSIC"}'::jsonb)
            FROM jsonb_array_elements(guesses::jsonb) AS elem
        )
              )
WHERE guesses IS NOT NULL
  AND jsonb_typeof(guesses::jsonb) = 'array'
  AND jsonb_array_length(guesses::jsonb) > 0;
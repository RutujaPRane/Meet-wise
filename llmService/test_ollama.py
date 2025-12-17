import ollama

response = ollama.chat(
    model='llama3.2',
    messages=[
        {
            'role': 'user',
            'content': 'Extract 2 action items from this: Alice will prepare the project timeline and Bob will review the budget. '
                       'Return JSON with a list called "items" where each has issueType, assignee, priority, description, summary.'
        }
    ],
)

print(response['message']['content'])

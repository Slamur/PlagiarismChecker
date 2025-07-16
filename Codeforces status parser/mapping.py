def read_status(status_filename):
    with open(status_filename, 'r') as fin:
        return [
            tuple(line.strip().split('\t'))
            for line in fin
        ]    


def parse_user_submissions(submission_infos):
    from collections import defaultdict

    user_submissions = defaultdict(lambda: defaultdict(dict))
    for submission_info in submission_infos:
        submission_id, date_str, user_id, problem_name, language_name, verdict_name, time_str, memory_str = submission_info

        problem_id = problem_name[:problem_name.index('-')].strip()

        user_submissions[user_id][problem_id][submission_id] = (date_str, language_name, verdict_name, time_str, memory_str)

    return user_submissions


def create_directories(user_submissions, root):
    from pathlib import Path

    from shutil import rmtree
    rmtree(Path(root), ignore_errors=True)

    for user_id, user_submissions in user_submissions.items():
        for problem_id in user_submissions:
            user_problem_dir_name = f'{root}/{user_id}/{problem_id}'
            Path(user_problem_dir_name).mkdir(parents=True, exist_ok=True)


def transfer_submissions(user_submissions, root, submissions_dir):
    language_file_extensions = {
        'C++': 'cpp',
        'Py': 'py',
    }

    verdicts = {
        'Accepted': 'AC',
        'Compilation': 'CE',
        'Wrong': 'WA',
        'Runtime': 'RE',
        'Time limit': 'TL',
        'Memory limit': 'ML',
    }

    id_to_target = dict()

    for user_id, user_submissions in user_submissions.items():
        for problem_id, problem_submissions in user_submissions.items():
            user_problem_dir_name = f'{root}/{user_id}/{problem_id}'

            for submission_id, submission_info in problem_submissions.items():
                date_str, language_name, verdict_name, time_str, memory_str = submission_info
                
                date_format_str = date_str.replace('/', '_').replace('UTC+', 'UTC+0')

                file_extension = [
                    extension
                    for prefix, extension in language_file_extensions.items()
                    if language_name[:len(prefix)] == prefix
                ][0]

                verdict = [
                    short_name
                    for prefix, short_name in verdicts.items()
                    if verdict_name[:len(prefix)] == prefix
                ][0]

                id_to_target[submission_id] = (
                    user_problem_dir_name,
                    f'{submission_id}-{date_format_str}-{verdict}.{file_extension}'
                )

    from pathlib import Path
    from shutil import copyfile

    submission_pathes = Path(submissions_dir).glob('*.*')
    for submission_path in submission_pathes:
        submission_name = str(submission_path.name)

        submission_id = submission_name[:submission_name.index('.')]
        submission_target = id_to_target.get(submission_id, None)
        if not submission_target:
            continue

        submission_target_dir, submission_target_name = submission_target

        submission_target_path = Path(f'{submission_target_dir}/{submission_target_name}')
        copyfile(submission_path, submission_target_path)

submission_infos = read_status('status.tsv')
user_submissions = parse_user_submissions(submission_infos)    

root_dir = 'auca-algos-25-part-1'
create_directories(user_submissions=user_submissions, root=root_dir)

transfer_submissions(user_submissions=user_submissions, root=root_dir, submissions_dir='submissions')
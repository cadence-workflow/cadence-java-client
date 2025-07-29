# Debug Github Actions integration test
Sometimes the environment on IDE may be different as Github Actions.
You can run the following command to trigger the same test as running on Github Actions:

```bash
docker-compose -f docker/github_actions/docker-compose.yml run unit-test-docker-sticky-on &> test.log
```
Or
```bash
docker-compose -f docker/github_actions/docker-compose.yml run unit-test-docker-sticky-off &> test.log
```

Or
```bash
docker-compose -f docker/github_actions/docker-compose.yml run unit-test-test-service &> test.log
```

And finally make sure to shutdown all docker resources:
```bash
docker-compose  down
```

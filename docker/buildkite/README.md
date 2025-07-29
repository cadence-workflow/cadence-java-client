# Debug Builkite integration test
Sometimes the environment on IDE may be different as Buildkite.
You can run the following command to trigger the same test as running on Buildkite:

```bash
docker-compose -f docker-compose.yaml run unit-test-docker-sticky-on &> test.log
```
Or
```bash
docker-compose -f docker-compose.yaml run unit-test-docker-sticky-off &> test.log
```

Or
```bash
docker-compose -f docker-compose.yaml run unit-test-test-service &> test.log
```

And finally make sure to shutdown all docker resources:
```bash
docker-compose  down
```
TODO: delete this file after migration to github actions

package com.nfl.dm.shield.graphql.registry.mutation;

import com.nfl.dm.shield.graphql.exception.GlitrValidationException;
import com.nfl.dm.shield.util.JsonUtils;
import com.nfl.dm.shield.util.ValidationUtil;
import com.nfl.dm.shield.web.exception.NFLValidationException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Validator;

import java.util.Map;

public class MutationDataFetcher implements DataFetcher {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(MutationDataFetcher.class);

    private final Class mutationInputClass;
    private final Validator validator;
    private final RelayMutation mutationFunc;


    public MutationDataFetcher(Class mutationInputClass, Validator validator, RelayMutation mutationFunc) {
        this.mutationInputClass = mutationInputClass;
        this.validator = validator;
        this.mutationFunc = mutationFunc;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(DataFetchingEnvironment env) {
        Map<String, Object> inputMap = env.getArgument("input");

        // map fields from input map to mutationInputClass
        Object inputPayloadMtn = JsonUtils.convertValue(inputMap, mutationInputClass);

        RelayMutationType mutationOutput;
        // apply some validation on inputPayloadMtn (should validation be in the mutationFunc instead?)
        validate(inputPayloadMtn);
        // mutate and return output
        mutationOutput = mutationFunc.call((RelayMutationType) inputPayloadMtn, env);
        // set back the client mutation id
        mutationOutput.setClientMutationId((String) inputMap.get("clientMutationId"));
        return mutationOutput;
    }

    private void validate(Object inputPayloadMtn) {
        if (validator == null) {
            return;
        }

        try {
            ValidationUtil.validOrThrowException(inputPayloadMtn, validator);
        } catch (NFLValidationException e) {
            throw new GlitrValidationException("Error validating input mutation.", e.getErrors());
        }
    }
}
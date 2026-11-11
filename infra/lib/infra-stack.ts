import * as cdk from 'aws-cdk-lib/core';
import { Construct } from 'constructs';
// import * as sqs from 'aws-cdk-lib/aws-sqs';
import { Key } from 'aws-cdk-lib/aws-kms';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import path from 'path';

declare const key: Key;

export class InfraStack extends cdk.Stack {
    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        // The code that defines your stack goes here

        // example resource
        // const queue = new sqs.Queue(this, 'InfraQueue', {
        //   visibilityTimeout: cdk.Duration.seconds(300)
        // });

        // https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_lambda-readme#aws-lambda-construct-library
        // https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_lambda-readme#bundling-asset-code

        // const bucket = new s3.Bucket(this, 'Bucket');

        // const options = {
        //     sourceKMSKey: key,
        // };

        const fn = new lambda.Function(this, 'myFunction2', {
            runtime: lambda.Runtime.JAVA_21,
            handler: 'not.used',
            code: lambda.Code.fromAsset(
                path.resolve(__dirname, '../../../services/api'),
                {
                    bundling: {
                        image: lambda.Runtime.JAVA_21.bundlingImage,
                        command: [
                            '/bin/sh',
                            '-c',
                            [
                                './gradlew bootJar --no-daemon',
                                'mkdir -p /asset-output',
                                'cp build/libs/*.jar /asset-output/app.jar',
                            ].join(' && '),
                        ],
                    },
                },
            ),
            memorySize: 1024,
            timeout: cdk.Duration.seconds(30),
        });
    }
}

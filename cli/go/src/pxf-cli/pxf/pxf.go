package pxf

import (
	"errors"
	"fmt"
	"os"

	"github.com/greenplum-db/gp-common-go-libs/cluster"
)

type EnvVar string

const (
	Gphome  EnvVar = "GPHOME"
	PxfConf EnvVar = "PXF_CONF"
)

type MessageType int

const (
	Success MessageType = iota
	Status
	Error
)

type Command struct {
	commandName     string
	messages        map[MessageType]string
	whereToRun      int
	requiredEnvVars []EnvVar
}

func (c *Command) RequiredEnvVars() []EnvVar {
	return c.requiredEnvVars
}

func (c *Command) WhereToRun() int {
	return c.whereToRun
}

func (c *Command) Messages(messageType MessageType) string {
	return c.messages[messageType]
}

func (c *Command) GetFunctionToExecute() (func(string) string, error) {
	inputs, err := makeValidCliInputs(c)
	if err != nil {
		return nil, err
	}

	switch c.commandName {
	case "sync":
		return func(hostname string) string {
			return fmt.Sprintf(
				"rsync -az -e 'ssh -o StrictHostKeyChecking=no' '%s/conf' '%s/lib' '%s/servers' '%s:%s'",
				inputs[PxfConf],
				inputs[PxfConf],
				inputs[PxfConf],
				hostname,
				inputs[PxfConf])
		}, nil
	default:
		pxfCommand := ""
		if inputs[PxfConf] != "" {
			pxfCommand += "PXF_CONF=" + inputs[PxfConf] + " "
		}
		pxfCommand += inputs[Gphome] + "/pxf/bin/pxf" + " " + c.commandName
		return func(_ string) string { return pxfCommand }, nil
	}
}

var (
	Init = Command{
		commandName: "init",
		messages: map[MessageType]string{
			Success: "PXF initialized successfully on %d out of %d hosts\n",
			Status:  "Initializing PXF on master and %d segment hosts...\n",
			Error:   "PXF failed to initialize on %d out of %d hosts\n",
		},
		requiredEnvVars: []EnvVar{Gphome, PxfConf},
		whereToRun:      cluster.ON_HOSTS_AND_MASTER,
	}
	Start = Command{
		commandName: "start",
		messages: map[MessageType]string{
			Success: "PXF started successfully on %d out of %d hosts\n",
			Status:  "Starting PXF on %d segment hosts...\n",
			Error:   "PXF failed to start on %d out of %d hosts\n",
		},
		requiredEnvVars: []EnvVar{Gphome},
		whereToRun:      cluster.ON_HOSTS,
	}
	Stop = Command{
		commandName: "stop",
		messages: map[MessageType]string{
			Success: "PXF stopped successfully on %d out of %d hosts\n",
			Status:  "Stopping PXF on %d segment hosts...\n",
			Error:   "PXF failed to stop on %d out of %d hosts\n",
		},
		requiredEnvVars: []EnvVar{Gphome},
		whereToRun:      cluster.ON_HOSTS,
	}
	Sync = Command{
		commandName: "sync",
		messages: map[MessageType]string{
			Success: "PXF configs synced successfully on %d out of %d hosts\n",
			Status:  "Syncing PXF configuration files to %d hosts...\n",
			Error:   "PXF configs failed to sync on %d out of %d hosts\n",
		},
		requiredEnvVars: []EnvVar{PxfConf},
		whereToRun:      cluster.ON_MASTER_TO_HOSTS,
	}
)

func makeValidCliInputs(c *Command) (map[EnvVar]string, error) {
	envVars := make(map[EnvVar]string)
	for _, e := range c.RequiredEnvVars() {
		val, err := validateEnvVar(e)
		if err != nil {
			return nil, err
		}
		envVars[e] = val
	}
	return envVars, nil
}

func validateEnvVar(envVar EnvVar) (string, error) {
	envVarValue, isEnvVarSet := os.LookupEnv(string(envVar))
	if !isEnvVarSet {
		return "", errors.New(string(envVar) + " must be set")
	}
	if envVarValue == "" {
		return "", errors.New(string(envVar) + " cannot be blank")
	}
	return envVarValue, nil
}
